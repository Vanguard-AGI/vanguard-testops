package io.vanguard.testops.workflow.support.socket;

import io.vanguard.testops.sdk.constants.MsgType;
import io.vanguard.testops.sdk.dto.SocketMsgDTO;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.sdk.util.LogUtils;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流执行 WebSocket 处理器
 * 用于实时推送工作流执行状态
 */
@Component
@ServerEndpoint("/ws/workflow/{runId}")
public class WorkflowWebSocketHandler {

    /**
     * 存储运行ID到Session的映射
     */
    public static final Map<String, Session> ONLINE_WORKFLOW_SESSIONS = new ConcurrentHashMap<>();

    /**
     * 发送消息
     */
    public static void sendMessage(Session session, SocketMsgDTO message) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.setMaxIdleTimeout(3600000); // 1小时超时
            RemoteEndpoint.Async async = session.getAsyncRemote();
            if (async == null) {
                return;
            }
            async.sendText(JSON.toJSONString(message));
        } catch (Exception e) {
            if (e instanceof ClosedChannelException 
                || (e instanceof IOException && e.getMessage() != null 
                    && e.getMessage().toLowerCase().contains("closed"))
                || (e.getMessage() != null && e.getMessage().toLowerCase().contains("closed"))) {
                LogUtils.debug("WebSocket连接已关闭，无法发送消息: {}", e.getMessage());
            } else {
                LogUtils.error("发送WebSocket消息失败: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 向指定运行ID发送消息
     */
    public static void sendMessageToRun(String runId, SocketMsgDTO message) {
        Session session = ONLINE_WORKFLOW_SESSIONS.get(runId);
        if (session != null) {
            sendMessage(session, message);
        }
    }

    /**
     * 发送步骤状态更新
     */
    public static void sendStepStatusUpdate(String runId, String stepId, String status, String description, Long duration) {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("stepId", stepId);
        data.put("runId", runId);
        data.put("status", status); // pending/running/success/failed
        data.put("description", description);
        data.put("duration", duration);
        data.put("timestamp", System.currentTimeMillis());
        
        SocketMsgDTO msg = new SocketMsgDTO(runId, "step_status", MsgType.EXEC_RESULT.name(), data);
        sendMessageToRun(runId, msg);
    }

    /**
     * 发送工作流状态更新
     */
    public static void sendWorkflowStatusUpdate(String runId, String status, String description, 
                                                Integer totalSteps, Integer passedCount, Integer failedCount) {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("runId", runId);
        data.put("status", status); // running/success/failed
        data.put("description", description);
        data.put("totalSteps", totalSteps);
        data.put("passedCount", passedCount);
        data.put("failedCount", failedCount);
        data.put("timestamp", System.currentTimeMillis());
        
        SocketMsgDTO msg = new SocketMsgDTO(runId, "workflow_status", MsgType.EXEC_RESULT.name(), data);
        sendMessageToRun(runId, msg);
    }

    /**
     * 发送完整的工作流执行结果（包含所有步骤数据）
     * 用于执行机回调后推送最终结果
     */
    public static void sendWorkflowResult(String runId, String status, String description, 
                                          List<Map<String, Object>> steps, String errorMsg) {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("runId", runId);
        data.put("status", status); // SUCCESS/FAILED/SUCCEED/FAIL
        data.put("description", description);
        data.put("steps", steps); // 所有步骤的执行明细
        if (errorMsg != null) {
            data.put("errorMsg", errorMsg);
        }
        data.put("timestamp", System.currentTimeMillis());
        
        SocketMsgDTO msg = new SocketMsgDTO(runId, "workflow_result", MsgType.EXEC_RESULT.name(), data);
        sendMessageToRun(runId, msg);
    }

    /**
     * 连接成功响应
     */
    @OnOpen
    public void openSession(@PathParam("runId") String runId, Session session) {
        ONLINE_WORKFLOW_SESSIONS.put(runId, session);
        
        RemoteEndpoint.Async async = session.getAsyncRemote();
        if (async != null) {
            SocketMsgDTO msg = new SocketMsgDTO(runId, "", MsgType.CONNECT.name(), MsgType.CONNECT.name());
            async.sendText(JSON.toJSONString(msg));
            session.setMaxIdleTimeout(3600000); // 1小时超时
        }
    }

    /**
     * 收到消息响应
     */
    @OnMessage
    public void onMessage(@PathParam("runId") String runId, String message) {
        // 可以处理客户端发送的消息，如取消执行等
    }

    /**
     * 连接关闭响应
     */
    @OnClose
    public void onClose(@PathParam("runId") String runId, Session session) {
        if (runId != null) {
            ONLINE_WORKFLOW_SESSIONS.remove(runId);
        }
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (Exception e) {
                LogUtils.debug("关闭WebSocket session失败: runId={}, error={}", runId, e.getMessage());
            }
        }
    }

    /**
     * 连接异常响应
     */
    @OnError
    public void onError(@PathParam("runId") String runId, Session session, Throwable throwable) {
        if (throwable instanceof ClosedChannelException 
            || (throwable instanceof IOException && throwable.getMessage() != null 
                && throwable.getMessage().toLowerCase().contains("closed"))) {
            LogUtils.debug("工作流WebSocket连接已关闭: runId={}, message={}", runId, throwable.getMessage());
        } else {
            LogUtils.error("工作流WebSocket连接异常: runId={}", runId, throwable);
        }
        
        if (runId != null) {
            ONLINE_WORKFLOW_SESSIONS.remove(runId);
        }
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (Exception e) {
                LogUtils.debug("关闭WebSocket session失败: runId={}, error={}", runId, e.getMessage());
            }
        }
    }

    /**
     * 心跳检查
     */
    @Scheduled(fixedRate = 60000)
    public void heartbeatCheck() {
        try {
            // 心跳消息发送给所有在线的工作流会话
            ONLINE_WORKFLOW_SESSIONS.forEach((runId, session) -> {
                try {
                    SocketMsgDTO msg = new SocketMsgDTO(runId, "", MsgType.HEARTBEAT.name(), "heartbeat check");
                    sendMessage(session, msg);
                } catch (Exception ignored) {
                }
            });
        } catch (Exception e) {
            LogUtils.error("心跳检查任务执行失败", e);
        }
    }
}
