package io.vanguard.testops.metadata.controller;

import io.vanguard.testops.metadata.dto.AnalyticsTrackRequest;
import io.vanguard.testops.metadata.dto.MetadataOperationLogDTO;
import io.vanguard.testops.metadata.service.MetadataOperationLogService;
import io.vanguard.testops.system.utils.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "埋点追踪")
@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    @Autowired
    private MetadataOperationLogService metadataOperationLogService;

    @PostMapping("/track")
    @Operation(summary = "埋点追踪接口")
    public void track(@Valid @RequestBody AnalyticsTrackRequest request) {
        if (request.getEvents() == null || request.getEvents().isEmpty()) {
            return;
        }
        metadataOperationLogService.trackEvents(request.getEvents());
    }

    @GetMapping("/recent")
    @Operation(summary = "查询当前用户最近执行的前5条数据（biz_type=1）")
    public List<MetadataOperationLogDTO> getRecentLogs(@RequestParam(required = false) String projectId) {
        io.vanguard.testops.system.dto.sdk.SessionUser user = SessionUtils.getUser();
        if (user == null || user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new RuntimeException("无法获取当前用户邮箱");
        }
        return metadataOperationLogService.getRecentLogsByUser(user.getEmail(), projectId, 5);
    }
}

