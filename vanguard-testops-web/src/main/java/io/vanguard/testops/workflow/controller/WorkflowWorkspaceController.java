package io.vanguard.testops.workflow.controller;

import io.vanguard.testops.system.utils.SessionUtils;
import io.vanguard.testops.workflow.dto.WorkflowWorkspaceCreateRequest;
import io.vanguard.testops.workflow.dto.WorkflowWorkspaceDTO;
import io.vanguard.testops.workflow.dto.WorkflowWorkspaceUpdateRequest;
import io.vanguard.testops.workflow.dto.WorkflowSyncImportRequest;
import io.vanguard.testops.workflow.dto.WorkflowSyncImportResponse;
import io.vanguard.testops.workflow.service.WorkflowWorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作流工作空间 Controller
 */
@RestController
@RequestMapping("/workflow/workspace")
@Tag(name = "工作流工作空间管理")
public class WorkflowWorkspaceController {

    @Resource
    private WorkflowWorkspaceService workflowWorkspaceService;

    @PostMapping("/create")
    @Operation(summary = "创建工作空间")
    public WorkflowWorkspaceDTO create(@RequestBody @Validated WorkflowWorkspaceCreateRequest request) {
        return workflowWorkspaceService.create(request, SessionUtils.getUserId());
    }

    @PostMapping("/update")
    @Operation(summary = "更新工作空间")
    public WorkflowWorkspaceDTO update(@RequestBody @Validated WorkflowWorkspaceUpdateRequest request) {
        return workflowWorkspaceService.update(request, SessionUtils.getUserId());
    }

    @GetMapping("/get/{workspaceId}")
    @Operation(summary = "获取工作空间详情")
    public WorkflowWorkspaceDTO get(@PathVariable String workspaceId) {
        return workflowWorkspaceService.get(workspaceId);
    }

    @GetMapping("/list")
    @Operation(summary = "获取工作空间列表（按项目ID）")
    public List<WorkflowWorkspaceDTO> getList(
            @RequestParam String projectId,
            @RequestParam(required = false) String keyword) {
        return workflowWorkspaceService.getListByProject(projectId, keyword);
    }

    @GetMapping("/list-by-user")
    @Operation(summary = "根据当前登录用户查询可访问的工作空间列表（包含工作流同步模块ID）")
    public List<WorkflowWorkspaceDTO> getListByUser() {
        String userId = SessionUtils.getUserId();
        String organizationId = SessionUtils.getCurrentOrganizationId();
        return workflowWorkspaceService.getListByUser(userId, organizationId);
    }

    @PostMapping("/sync-import")
    @Operation(summary = "工作流同步导入：将HTTP和SQL请求转换为工作流节点并导入")
    public WorkflowSyncImportResponse syncImport(@Validated @RequestBody WorkflowSyncImportRequest request) {
        return workflowWorkspaceService.syncImport(request, SessionUtils.getUserId());
    }

    @PostMapping("/copy/{workspaceId}")
    @Operation(summary = "复制工作空间")
    public WorkflowWorkspaceDTO copy(@PathVariable String workspaceId) {
        return workflowWorkspaceService.copy(workspaceId, SessionUtils.getUserId());
    }

    @GetMapping("/delete/{workspaceId}")
    @Operation(summary = "删除工作空间")
    public void delete(@PathVariable String workspaceId) {
        workflowWorkspaceService.delete(workspaceId, SessionUtils.getUserId());
    }
}

