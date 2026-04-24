package io.vanguard.testops.functional.controller;

import io.vanguard.testops.functional.service.FeishuMeegoService;
import io.vanguard.testops.sdk.util.LogUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 飞书Meego Webhook回调控制器
 */
@RestController
@RequestMapping("/api/callback/meego")
@Tag(name = "飞书Meego Webhook")
public class MeegoWebhookController {

    @Resource
    private FeishuMeegoService feishuMeegoService;

    @Value("${feishu.meego.default-user-email:}")
    private String defaultUserEmail;

    /**
     * 接收飞书Meego的Webhook回调
     */
    @PostMapping
    @Operation(summary = "飞书Meego Webhook回调")
    public ResponseEntity<Map<String, Object>> handleMeegoWebhook(@RequestBody Map<String, Object> payload) {
        try {
            if (payload == null || payload.isEmpty()) {
                return ResponseEntity.ok(Map.of("code", 0, "msg", "empty payload"));
            }

            String eventKey = (String) payload.getOrDefault("event_key", payload.get("type"));
            
            if (StringUtils.isBlank(eventKey) || !eventKey.startsWith("work_item")) {
                return ResponseEntity.ok(Map.of("code", 0, "msg", "ignored event"));
            }

            LogUtils.info("收到飞书Meego Webhook: {}", eventKey);

            Map<String, Object> eventData = null;
            if (payload.get("event") instanceof Map) {
                eventData = (Map<String, Object>) payload.get("event");
            } else if (payload.get("data") instanceof Map) {
                eventData = (Map<String, Object>) payload.get("data");
            }

            if (eventData == null) {
                return ResponseEntity.ok(Map.of("code", 0, "msg", "success"));
            }

            // 从 Webhook 数据中获取项目 key 和工作项 ID
            String projectKey = (String) eventData.get("project_key");
            String workItemId = (String) eventData.get("work_item_id");
            String workItemTypeKey = (String) eventData.get("work_item_type_key");

            if (StringUtils.isBlank(projectKey) || StringUtils.isBlank(workItemId)) {
                return ResponseEntity.ok(Map.of("code", 0, "msg", "success"));
            }

            if ("defect".equals(workItemTypeKey)) {
                LogUtils.info("检测到缺陷变更，项目Key: {}, 工作项ID: {}", projectKey, workItemId);
                
                if (StringUtils.isNotBlank(defaultUserEmail)) {
                    // 使用异步方法更新（不阻塞Webhook响应）
                    try {
                        feishuMeegoService.updateStoryDefectCount(projectKey, workItemId, defaultUserEmail);
                        LogUtils.info("缺陷更新任务已提交至异步线程池");
                    } catch (Exception e) {
                        LogUtils.info("提交缺陷更新任务失败 projectKey={}, workItemId={}", projectKey, workItemId, e);
                    }
                } else {
                    LogUtils.warn("未配置 default-user-email，无法执行更新");
                }
            }

            return ResponseEntity.ok(Map.of("code", 0, "msg", "success"));
            
        } catch (Exception e) {
            LogUtils.error("处理Meego Webhook异常", e);
            return ResponseEntity.ok(Map.of("code", 0, "msg", "error handled"));
        }
    }

    /**
     * 手动触发全量同步（异步执行，立即返回）
     * @param userEmail 用户邮箱，例如：jan.zhang@spotterio.com
     */
    @PostMapping("/sync")
    @Operation(summary = "手动触发需求全量同步（异步执行）")
    public ResponseEntity<Map<String, Object>> manualSync(@RequestParam String userEmail) {
        try {
            if (StringUtils.isBlank(userEmail)) {
                return ResponseEntity.badRequest().body(Map.of("code", -1, "msg", "userEmail不能为空"));
            }

            // 使用配置的异步线程池执行
            feishuMeegoService.syncStoriesAsync(userEmail);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 0);
            result.put("msg", "同步任务已提交至后台异步执行");
            result.put("async", true);
            result.put("tip", "任务正在后台执行，不会阻塞其他操作，请稍后查看结果");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LogUtils.error("触发同步失败", e);
            return ResponseEntity.ok(Map.of("code", -1, "msg", "启动失败: " + e.getMessage()));
        }
    }
}
