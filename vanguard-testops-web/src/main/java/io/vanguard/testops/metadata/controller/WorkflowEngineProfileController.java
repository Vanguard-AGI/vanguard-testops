package io.vanguard.testops.metadata.controller;

import io.vanguard.testops.metadata.dto.*;
import io.vanguard.testops.metadata.service.WorkflowEngineProfileService;
import io.vanguard.testops.sdk.constants.PermissionConstants;
import io.vanguard.testops.system.log.annotation.Log;
import io.vanguard.testops.system.log.constants.OperationLogType;
import io.vanguard.testops.system.security.annotation.CheckOwner;
import io.vanguard.testops.system.utils.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作流引擎配置控制器
 */
@RestController
@RequestMapping("/user/profile")
@Tag(name = "用户-环境配置")
public class WorkflowEngineProfileController {

    @Resource
    private WorkflowEngineProfileService workflowEngineProfileService;

    @PostMapping("/add")
    @Operation(summary = "创建环境配置")
    // @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_ADD)
    @Log(type = OperationLogType.ADD, expression = "")
    // @CheckOwner(resourceId = "#request.getProjectId()", resourceType = "project")
    public String add(@Validated @RequestBody WorkflowEngineProfileAddRequest request) {
        String userId = SessionUtils.getUserId();
        if (userId == null) {
            userId = "anonymous";
        }
        return workflowEngineProfileService.create(request, userId);
    }

    @PostMapping("/update")
    @Operation(summary = "更新环境配置")
    // @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_UPDATE)
    @Log(type = OperationLogType.UPDATE, expression = "")
    public void update(@Validated @RequestBody WorkflowEngineProfileUpdateRequest request) {
        String userId = SessionUtils.getUserId();
        if (userId == null) {
            userId = "anonymous";
        }
        workflowEngineProfileService.update(request, userId);
    }

    @GetMapping("/delete/{id}")
    @Operation(summary = "删除环境配置")
    // @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_DELETE)
    @Log(type = OperationLogType.DELETE, expression = "")
    public void delete(@PathVariable String id) {
        String userId = SessionUtils.getUserId();
        if (userId == null) {
            userId = "anonymous";
        }
        workflowEngineProfileService.delete(id, userId);
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "获取环境配置详情")
    // @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_READ)
    public WorkflowEngineProfileDTO get(@PathVariable String id) {
        return workflowEngineProfileService.get(id);
    }

    @PostMapping("/page")
    @Operation(summary = "分页查询环境配置列表")
    // @RequiresPermissions(PermissionConstants.PROJECT_API_DEFINITION_READ)
    // @CheckOwner(resourceId = "#request.getProjectId()", resourceType = "project")
    public List<WorkflowEngineProfileDTO> page(@Validated @RequestBody WorkflowEngineProfilePageRequest request) {
        return workflowEngineProfileService.list(request);
    }
}

