package io.vanguard.testops.project.controller;


import io.vanguard.testops.project.dto.environment.EnvironmentFilterRequest;
import io.vanguard.testops.project.dto.environment.EnvironmentGroupDTO;
import io.vanguard.testops.project.dto.environment.EnvironmentGroupRequest;
import io.vanguard.testops.project.service.EnvironmentGroupLogService;
import io.vanguard.testops.project.service.EnvironmentGroupService;
import io.vanguard.testops.sdk.constants.PermissionConstants;
import io.vanguard.testops.sdk.domain.EnvironmentGroup;
import io.vanguard.testops.system.dto.sdk.OptionDTO;
import io.vanguard.testops.system.dto.sdk.request.PosRequest;
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

@RestController
@RequestMapping(value = "/project/environment/group")
@Tag(name = "项目管理-环境组")
public class EnvironmentGroupController {

    @Resource
    private EnvironmentGroupService environmentGroupService;

    @PostMapping("/add")
    @Operation(summary = "项目管理-环境组-新增")
    @RequiresPermissions(PermissionConstants.PROJECT_ENVIRONMENT_READ_ADD)
    @Log(type = OperationLogType.ADD, expression = "#msClass.addLog(#request)", msClass = EnvironmentGroupLogService.class)
    public EnvironmentGroup add(@Validated @RequestBody EnvironmentGroupRequest request) {
        return environmentGroupService.add(request, SessionUtils.getUserId());
    }

    @GetMapping("/delete/{id}")
    @Operation(summary = "项目管理-环境组-删除")
    @RequiresPermissions(PermissionConstants.PROJECT_ENVIRONMENT_READ_DELETE)
    @CheckOwner(resourceId = "#id", resourceType = "environment_group")
    @Log(type = OperationLogType.DELETE, expression = "#msClass.deleteLog(#id)", msClass = EnvironmentGroupLogService.class)
    public void delete(@PathVariable String id) {
        environmentGroupService.delete(id);
    }

    @PostMapping("/update")
    @Operation(summary = "项目管理-环境组-修改")
    @RequiresPermissions(PermissionConstants.PROJECT_ENVIRONMENT_READ_UPDATE)
    @CheckOwner(resourceId = "#request.id", resourceType = "environment_group")
    @Log(type = OperationLogType.UPDATE, expression = "#msClass.updateLog(#request)", msClass = EnvironmentGroupLogService.class)
    public EnvironmentGroup update(@Validated @RequestBody EnvironmentGroupRequest request) {
        return environmentGroupService.update(request, SessionUtils.getUserId());
    }

    @PostMapping("/list")
    @Operation(summary = "项目管理-环境组-列表")
    @RequiresPermissions(PermissionConstants.PROJECT_ENVIRONMENT_READ)
    @CheckOwner(resourceId = "#request.projectId", resourceType = "project")
    public List<EnvironmentGroup> list(@RequestBody EnvironmentFilterRequest request) {
        return environmentGroupService.list(request);
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "项目管理-环境组-详情")
    @RequiresPermissions(PermissionConstants.PROJECT_ENVIRONMENT_READ)
    @CheckOwner(resourceId = "#id", resourceType = "environment_group")
    public EnvironmentGroupDTO get(@PathVariable String id) {
        return environmentGroupService.get(id);
    }

    @GetMapping("/get-project/{organizationId}")
    @Operation(summary = "项目管理-环境组-获取项目")
    @RequiresPermissions(PermissionConstants.PROJECT_ENVIRONMENT_READ)
    @CheckOwner(resourceId = "#organizationId", resourceType = "organization")
    public List<OptionDTO> getProject(@PathVariable String organizationId) {
        return environmentGroupService.getProject(SessionUtils.getUserId(), organizationId);
    }

    @PostMapping("/edit/pos")
    @Operation(summary = "项目管理-环境-环境组-修改排序")
    @RequiresPermissions(PermissionConstants.PROJECT_ENVIRONMENT_READ_UPDATE)
    public void editPos(@Validated @RequestBody PosRequest request) {
        environmentGroupService.moveNode(request);
    }

}
