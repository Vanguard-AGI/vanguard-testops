package io.vanguard.testops.workflow.controller;

import io.vanguard.testops.workflow.dto.PluginSyncRequest;
import io.vanguard.testops.workflow.dto.PluginSyncResponse;
import io.vanguard.testops.workflow.service.WorkflowPluginSyncNodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * Aegis 同步接口 Controller
 * 用于外部插件同步节点数据，无需鉴权
 */
@Tag(name = "Aegis 同步接口")
@RestController
@RequestMapping("/api/aegis")
public class AegisSyncController {

    @Resource
    private WorkflowPluginSyncNodeService pluginSyncNodeService;

    /**
     * 同步节点数据接口
     * 外部插件调用此接口同步节点数据到平台
     * @param request 同步请求
     * @return 同步响应
     */
    @PostMapping("/sync")
    @Operation(summary = "同步节点数据（外部插件调用）")
    public PluginSyncResponse sync(@Valid @RequestBody PluginSyncRequest request) {
        return pluginSyncNodeService.syncNodes(request);
    }
}

