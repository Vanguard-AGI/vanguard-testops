package io.vanguard.testops.bug.controller;

import io.vanguard.testops.bug.service.FeishuWebhookService;
import io.vanguard.testops.sdk.util.LogUtils;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 飞书 Webhook 回调接收端点。
 * 无需鉴权 —— 通过请求体中的 challenge/token 完成校验。
 */
@RestController
@RequestMapping("/webhook/feishu")
public class FeishuWebhookController {

    @Resource
    private FeishuWebhookService feishuWebhookService;

    @PostMapping("/bug")
    public ResponseEntity<Object> handleBugEvent(@RequestBody Map<String, Object> payload) {
        // 调试：只打 body 的 key 和 challenge，避免日志过长
        LogUtils.info("[FeishuWebhook] 收到回调, body.keys={}", payload != null ? payload.keySet() : "null");

        // 飞书 URL 验证（challenge 握手）
        if (payload != null && payload.containsKey("challenge")) {
            LogUtils.info("[FeishuWebhook] url_verification challenge, 返回 challenge");
            return ResponseEntity.ok(Map.of("challenge", payload.get("challenge")));
        }

        try {
            feishuWebhookService.dispatch(payload);
        } catch (Exception e) {
            LogUtils.error("[FeishuWebhook] 处理事件异常: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok(Map.of("code", 0));
    }
}
