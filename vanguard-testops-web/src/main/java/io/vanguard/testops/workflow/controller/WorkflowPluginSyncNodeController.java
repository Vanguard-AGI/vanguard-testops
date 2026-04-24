package io.vanguard.testops.workflow.controller;

import io.vanguard.testops.system.utils.SessionUtils;
import io.vanguard.testops.workflow.dto.PluginSyncNodeDTO;
import io.vanguard.testops.workflow.dto.PluginSyncNodeUpdateRequest;
import io.vanguard.testops.workflow.dto.PluginSyncRequest;
import io.vanguard.testops.workflow.dto.PluginSyncResponse;
import io.vanguard.testops.workflow.service.WorkflowPluginSyncNodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 外部插件同步节点数据 Controller
 */
@Tag(name = "外部插件同步节点管理")
@RestController
@RequestMapping("/workflow/plugin-sync-node")
public class WorkflowPluginSyncNodeController {

    @Resource
    private WorkflowPluginSyncNodeService pluginSyncNodeService;

    /**
     * 根据当前用户邮箱查询节点列表
     * @return 节点列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取当前用户的插件同步节点列表")
    public List<PluginSyncNodeDTO> getNodesByCurrentUser() {
        // 获取当前登录用户的邮箱
        io.vanguard.testops.system.dto.sdk.SessionUser user = SessionUtils.getUser();
        if (user == null || user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new RuntimeException("无法获取当前用户邮箱");
        }
        return pluginSyncNodeService.getNodesByEmail(user.getEmail());
    }

    /**
     * 根据当前用户邮箱和节点类型查询节点列表
     * @param nodeType 节点类型（HTTP/SQL）
     * @return 节点列表
     */
    @GetMapping("/list/{nodeType}")
    @Operation(summary = "根据节点类型获取当前用户的插件同步节点列表")
    public List<PluginSyncNodeDTO> getNodesByCurrentUserAndType(@PathVariable String nodeType) {
        io.vanguard.testops.system.dto.sdk.SessionUser user = SessionUtils.getUser();
        if (user == null || user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new RuntimeException("无法获取当前用户邮箱");
        }
        return pluginSyncNodeService.getNodesByEmailAndType(user.getEmail(), nodeType);
    }

    /**
     * 更新节点数据
     * @param request 更新请求
     * @return 更新后的节点DTO
     */
    @PostMapping("/update")
    @Operation(summary = "更新插件同步节点数据")
    public PluginSyncNodeDTO updateNode(@Valid @RequestBody PluginSyncNodeUpdateRequest request) {
        io.vanguard.testops.system.dto.sdk.SessionUser user = SessionUtils.getUser();
        if (user == null || user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new RuntimeException("无法获取当前用户邮箱");
        }
        return pluginSyncNodeService.updateNode(request.getNodeId(), user.getEmail(), request.getEndpointData());
    }
}

