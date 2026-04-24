package io.vanguard.testops.workflow.controller;

import io.vanguard.testops.sdk.constants.PermissionConstants;
import io.vanguard.testops.system.security.annotation.CheckOwner;
import io.vanguard.testops.system.utils.SessionUtils;
import io.vanguard.testops.workflow.dto.*;
import io.vanguard.testops.workflow.service.WorkflowDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作流定义 Controller
 * 提供工作流的 CRUD 接口
 */
@Tag(name = "工作流定义管理")
@RestController
@RequestMapping("/workflow/definition")
public class WorkflowDefinitionController {

    @Resource
    private WorkflowDefinitionService workflowDefinitionService;

    /**
     * 保存工作流（创建或更新）
     * 同时保存节点列表（包含坐标）和连线列表（包含样式）
     */
    @PostMapping("/save")
    @Operation(summary = "保存工作流")
    @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_ADD)
    @CheckOwner(resourceId = "#request.projectId", resourceType = "project")
    public String save(@Validated @RequestBody WorkflowDefinitionSaveRequest request) {
        String userId = SessionUtils.getUserId();
        return workflowDefinitionService.save(request, userId);
    }

    /**
     * 获取工作流详情
     * 返回工作流定义、节点列表（恢复坐标）、连线列表（恢复样式）
     */
    @GetMapping("/get/{workflowId}")
    @Operation(summary = "获取工作流详情")
    @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_READ)
    public WorkflowDefinitionDTO get(@PathVariable String workflowId) {
        return workflowDefinitionService.get(workflowId);
    }

    /**
     * 分页查询工作流列表
     */
    @PostMapping("/page")
    @Operation(summary = "分页查询工作流列表")
    @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_READ)
    @CheckOwner(resourceId = "#request.projectId", resourceType = "project")
    public List<WorkflowDefinitionListItem> page(@Validated @RequestBody WorkflowDefinitionPageRequest request) {
        return workflowDefinitionService.list(request);
    }

    /**
     * 删除工作流（软删除）
     */
    @GetMapping("/delete/{workflowId}")
    @Operation(summary = "删除工作流")
    @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_DELETE)
    public void delete(@PathVariable String workflowId) {
        String userId = SessionUtils.getUserId();
        workflowDefinitionService.delete(workflowId, userId);
    }

    /**
     * 复制工作流
     */
    @PostMapping("/copy/{workflowId}")
    @Operation(summary = "复制工作流")
    @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_ADD)
    public String copy(@PathVariable String workflowId) {
        String userId = SessionUtils.getUserId();
        return workflowDefinitionService.copy(workflowId, userId);
    }

    /**
     * 批量复制工作流到指定模块
     */
    @PostMapping("/batch/copy")
    @Operation(summary = "批量复制工作流到指定模块")
    @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_ADD)
    public List<String> batchCopy(@Validated @RequestBody WorkflowBatchCopyRequest request) {
        String userId = SessionUtils.getUserId();
        return workflowDefinitionService.batchCopy(request, userId);
    }

    /**
     * 批量移动工作流到指定模块
     */
    @PostMapping("/batch/move")
    @Operation(summary = "批量移动工作流到指定模块")
    @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_UPDATE)
    public void batchMove(@Validated @RequestBody WorkflowBatchMoveRequest request) {
        String userId = SessionUtils.getUserId();
        workflowDefinitionService.batchMove(request, userId);
    }
}

