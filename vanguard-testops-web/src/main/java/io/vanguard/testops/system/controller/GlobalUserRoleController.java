package io.vanguard.testops.system.controller;

import io.vanguard.testops.sdk.constants.PermissionConstants;
import io.vanguard.testops.sdk.util.BeanUtils;
import io.vanguard.testops.system.domain.UserRole;
import io.vanguard.testops.system.dto.permission.PermissionDefinitionItem;
import io.vanguard.testops.system.dto.sdk.request.PermissionSettingUpdateRequest;
import io.vanguard.testops.system.dto.sdk.request.UserRoleUpdateRequest;
import io.vanguard.testops.system.log.annotation.Log;
import io.vanguard.testops.system.log.constants.OperationLogType;
import io.vanguard.testops.system.service.GlobalUserRoleLogService;
import io.vanguard.testops.system.service.GlobalUserRoleService;
import io.vanguard.testops.system.utils.SessionUtils;
import io.vanguard.testops.validation.groups.Created;
import io.vanguard.testops.validation.groups.Updated;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author : Jan
 * @date : 2026-04-22
 */
@Tag(name = "系统设置-系统-用户组")
@RestController
@RequestMapping("/user/role/global")
public class GlobalUserRoleController {

    @Resource
    private GlobalUserRoleService globalUserRoleService;

    @GetMapping("/list")
    @Operation(summary = "系统设置-系统-用户组-获取全局用户组列表")
    @RequiresPermissions(PermissionConstants.SYSTEM_USER_ROLE_READ)
    public List<UserRole> list() {
        return globalUserRoleService.list();
    }

    @GetMapping("/permission/setting/{id}")
    @Operation(summary = "系统设置-系统-用户组-获取全局用户组对应的权限配置")
    @RequiresPermissions(PermissionConstants.SYSTEM_USER_ROLE_READ)
    public List<PermissionDefinitionItem> getPermissionSetting(@PathVariable String id) {
        return globalUserRoleService.getPermissionSetting(id);
    }

    @PostMapping("/permission/update")
    @Operation(summary = "系统设置-系统-用户组-编辑全局用户组对应的权限配置")
    @RequiresPermissions(PermissionConstants.SYSTEM_USER_ROLE_UPDATE)
    @Log(type = OperationLogType.UPDATE, expression = "#msClass.updateLog(#request)", msClass = GlobalUserRoleLogService.class)
    public void updatePermissionSetting(@Validated @RequestBody PermissionSettingUpdateRequest request) {
        globalUserRoleService.updatePermissionSetting(request);
    }

    @PostMapping("/add")
    @Operation(summary = "系统设置-系统-用户组-添加自定义全局用户组")
    @RequiresPermissions(PermissionConstants.SYSTEM_USER_ROLE_ADD)
    @Log(type = OperationLogType.ADD, expression = "#msClass.addLog(#request)", msClass = GlobalUserRoleLogService.class)
    public UserRole add(@Validated({Created.class}) @RequestBody UserRoleUpdateRequest request) {
        UserRole userRole = new UserRole();
        userRole.setCreateUser(SessionUtils.getUserId());
        BeanUtils.copyBean(userRole, request);
        return globalUserRoleService.add(userRole);
    }

    @PostMapping("/update")
    @Operation(summary = "系统设置-系统-用户组-更新自定义全局用户组")
    @RequiresPermissions(PermissionConstants.SYSTEM_USER_ROLE_UPDATE)
    @Log(type = OperationLogType.UPDATE, expression = "#msClass.updateLog(#request)", msClass = GlobalUserRoleLogService.class)
    public UserRole update(@Validated({Updated.class}) @RequestBody UserRoleUpdateRequest request) {
        UserRole userRole = new UserRole();
        BeanUtils.copyBean(userRole, request);
        return globalUserRoleService.update(userRole);
    }

    @GetMapping("/delete/{id}")
    @Operation(summary = "系统设置-系统-用户组-删除自定义全局用户组")
    @RequiresPermissions(PermissionConstants.SYSTEM_USER_ROLE_DELETE)
    @Log(type = OperationLogType.DELETE, expression = "#msClass.deleteLog(#id)", msClass = GlobalUserRoleLogService.class)
    public void delete(@PathVariable String id) {
        globalUserRoleService.delete(id, SessionUtils.getUserId());
    }
}
