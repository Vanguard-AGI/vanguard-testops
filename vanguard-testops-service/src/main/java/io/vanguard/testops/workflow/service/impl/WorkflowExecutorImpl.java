package io.vanguard.testops.workflow.service.impl;

import io.vanguard.testops.workflow.dto.WorkflowRunStepDTO;
import io.vanguard.testops.workflow.service.ExecutionContext;
import io.vanguard.testops.workflow.service.WorkflowExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 工作流执行器实现类
 *
 * <p>Current architecture note: this class is a local execution skeleton and is
 * not the active runtime execution path. The live path is:
 * {@code WorkflowRunService -> remote executor -> callback -> WebSocket/status update}.</p>
 *
 * <p>Keep this class honest as a not-yet-integrated skeleton. Do not add fake step
 * execution branches here unless the project decides to restore in-process
 * execution as a real supported mode.</p>
 */
@Slf4j
@Service
public class WorkflowExecutorImpl implements WorkflowExecutor {

    @Override
    @Async
    public void executeAsync(String runId, String workflowId, String environmentId) {
        log.error(
            "WorkflowExecutorImpl.executeAsync 被调用，但当前运行时不支持本地工作流执行: workflowId={}, runId={}, environmentId={}",
            workflowId, runId, environmentId
        );
        throw new UnsupportedOperationException(
            "当前工作流运行时使用 WorkflowRunService 调度远端执行机；" +
                "WorkflowExecutorImpl.executeAsync 仍是未接入的本地执行骨架"
        );
    }

    @Override
    public WorkflowRunStepDTO executeStep(String stepId, ExecutionContext context) {
        throw new UnsupportedOperationException(
            "当前工作流运行时使用远端执行机，不走 WorkflowExecutorImpl 本地执行链；" +
                "如需启用本地执行，请先接入真实 StepExecutor 实现与结果持久化链路"
        );
    }

    @Override
    public void cancel(String runId) {
        throw new UnsupportedOperationException(
            "当前取消链路由 WorkflowRunService 负责状态更新与前端通知；" +
                "WorkflowExecutorImpl 本地取消逻辑尚未接入运行时"
        );
    }
}
