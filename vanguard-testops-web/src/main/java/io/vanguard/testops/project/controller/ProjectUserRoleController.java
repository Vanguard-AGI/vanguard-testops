package io.vanguard.testops.project.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.vanguard.testops.project.dto.ProjectUserRoleDTO;
import io.vanguard.testops.project.request.ProjectUserRoleEditRequest;
import io.vanguard.testops.project.request.ProjectUserRoleMemberEditRequest;
import io.vanguard.testops.project.request.ProjectUserRoleMemberRequest;
import io.vanguard.testops.project.request.ProjectUserRoleRequest;
import io.vanguard.testops.project.service.ProjectUserRoleLogService;
import io.vanguard.testops.project.service.ProjectUserRoleService;
import io.vanguard.testops.sdk.constants.PermissionConstants;
import io.vanguard.testops.sdk.util.BeanUtils;
import io.vanguard.testops.system.domain.User;
import io.vanguard.testops.system.domain.UserRole;
import io.vanguard.testops.system.dto.permission.PermissionDefinitionItem;
import io.vanguard.testops.system.dto.sdk.request.PermissionSettingUpdateRequest;
import io.vanguard.testops.system.dto.user.UserExtendDTO;
import io.vanguard.testops.system.log.annotation.Log;
import io.vanguard.testops.system.log.constants.OperationLogType;
import io.vanguard.testops.system.security.annotation.CheckProjectOwner;
import io.vanguard.testops.system.service.UserRoleService;
import io.vanguard.testops.system.support.page.PageUtils;
import io.vanguard.testops.system.dto.page.Pager;
import io.vanguard.testops.system.utils.SessionUtils;
import io.vanguard.testops.validation.groups.Created;
import io.vanguard.testops.validation.groups.Updated;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "项目管理-项目与权限-用户组")
@RestController
@RequestMapping("/user/role/project")
public class ProjectUserRoleController {

    @Resource
    ProjectUserRoleService projectUserRoleService;
    @Resource
    UserRoleService userRoleService;


    @PostMapping("/list")
    @Operation(summary = "项目管理-项目与权限-用户组-获取用户组列表")
    @RequiresPermissions(PermissionConstants.PROJECT_GROUP_READ)
    public Pager<List<ProjectUserRoleDTO>> list(@Validated @RequestBody ProjectUserRoleRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return PageUtils.setPageInfo(page, projectUserRoleService.list(request));
    }

    @PostMapping("/add")
    @Operation(summary = "项目管理-项目与权限-用户组-添加用户组")
    @RequiresPermissions(PermissionConstants.PROJECT_GROUP_ADD)
    @Log(type = OperationLogType.ADD, expression = "#msClass.addLog(#request)", msClass = ProjectUserRoleLogService.class)
    public UserRole add(@Validated({Created.class}) @RequestBody ProjectUserRoleEditRequest request) {
        UserRole userRole = new UserRole();
        userRole.setCreateUser(SessionUtils.getUserId());
        BeanUtils.copyBean(userRole, request);
        return projectUserRoleService.add(userRole);
    }

    @PostMapping("/update")
    @Operation(summary = "项目管理-项目与权限-用户组-修改用户组")
    @RequiresPermissions(PermissionConstants.PROJECT_GROUP_UPDATE)
    @Log(type = OperationLogType.UPDATE, expression = "#msClass.updateLog(#request)", msClass = ProjectUserRoleLogService.class)
    @CheckProjectOwner(resourceId = "#request.getId()", resourceType = "user_role", resourceCol = "scope_id")
    public UserRole update(@Validated({Updated.class}) @RequestBody ProjectUserRoleEditRequest request) {
        UserRole userRole = new UserRole();
        BeanUtils.copyBean(userRole, request);
        return projectUserRoleService.update(userRole);
    }

    @GetMapping("/delete/{id}")
    @Operation(summary = "项目管理-项目与权限-用户组-删除用户组")
    @RequiresPermissions(PermissionConstants.PROJECT_GROUP_DELETE)
    @Parameter(name = "id", description = "用户组ID", schema = @Schema(requiredMode = Schema.RequiredMode.REQUIRED))
    @Log(type = OperationLogType.DELETE, expression = "#msClass.deleteLog(#id)", msClass = ProjectUserRoleLogService.class)
    @CheckProjectOwner(resourceId = "#id", resourceType = "user_role", resourceCol = "scope_id")
    public void delete(@PathVariable String id) {
        projectUserRoleService.delete(id, SessionUtils.getUserId());
    }

    @GetMapping("/permission/setting/{id}")
    @Operation(summary = "项目管理-项目与权限-用户组-获取用户组对应的权限配置")
    @Parameter(name = "id", description = "用户组ID", schema = @Schema(requiredMode = Schema.RequiredMode.REQUIRED))
    @RequiresPermissions(PermissionConstants.PROJECT_GROUP_READ)
    public List<PermissionDefinitionItem> getPermissionSetting(@PathVariable String id) {
        return projectUserRoleService.getPermissionSetting(id);
    }

    @PostMapping("/permission/update")
    @Operation(summary = "项目管理-项目与权限-用户组-修改用户组对应的权限配置")
    @RequiresPermissions(PermissionConstants.PROJECT_GROUP_UPDATE)
    @Log(type = OperationLogType.UPDATE, expression = "#msClass.updatePermissionSettingLog(#request)", msClass = ProjectUserRoleLogService.class)
    @CheckProjectOwner(resourceId = "#request.getUserRoleId()", resourceType = "user_role", resourceCol = "scope_id")
    public void updatePermissionSetting(@Validated @RequestBody PermissionSettingUpdateRequest request) {
        projectUserRoleService.updatePermissionSetting(request);
    }

    @GetMapping("/get-member/option/{projectId}/{roleId}")
    @Operation(summary = "项目管理-项目与权限-用户组-获取成员下拉选项")
    @RequiresPermissions(value = {PermissionConstants.PROJECT_GROUP_READ})
    @Parameters({
            @Parameter(name = "projectId", description = "当前项目ID", schema = @Schema(requiredMode = Schema.RequiredMode.REQUIRED)),
            @Parameter(name = "roleId", description = "用户组ID", schema = @Schema(requiredMode = Schema.RequiredMode.REQUIRED))
    })
    public List<UserExtendDTO> getMember(@PathVariable String projectId,
                                         @PathVariable String roleId,
                                         @Schema(description = "查询关键字，根据邮箱和用户名查询")
                                      @RequestParam(required = false) String keyword) {
        return userRoleService.getMember(projectId, roleId, keyword);
    }

    @PostMapping("/list-member")
    @Operation(summary = "项目管理-项目与权限-用户组-获取成员列表")
    @RequiresPermissions(value = {PermissionConstants.PROJECT_GROUP_READ})
    public Pager<List<User>> listMember(@Validated @RequestBody ProjectUserRoleMemberRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return PageUtils.setPageInfo(page, projectUserRoleService.listMember(request));
    }

    @PostMapping("/add-member")
    @Operation(summary = "项目管理-项目与权限-用户组-添加用户组成员")
    @RequiresPermissions(PermissionConstants.PROJECT_GROUP_UPDATE)
    @Log(type = OperationLogType.UPDATE, expression = "#msClass.editMemberLog(#request)", msClass = ProjectUserRoleLogService.class)
    public void addMember(@Validated @RequestBody ProjectUserRoleMemberEditRequest request) {
        projectUserRoleService.addMember(request, SessionUtils.getUserId());
    }

    @PostMapping("/remove-member")
    @Operation(summary = "项目管理-项目与权限-用户组-删除用户组成员")
    @RequiresPermissions(PermissionConstants.PROJECT_GROUP_UPDATE)
    @Log(type = OperationLogType.UPDATE, expression = "#msClass.editMemberLog(#request)", msClass = ProjectUserRoleLogService.class)
    public void removeMember(@Validated @RequestBody ProjectUserRoleMemberEditRequest request) {
        projectUserRoleService.removeMember(request);
    }

    @GetMapping("/get-project-by-email/{email}")
    @Operation(summary = "项目管理-项目与权限-用户组-通过邮箱查询用户ID和项目ID列表")
    @Parameter(name = "email", description = "用户邮箱", schema = @Schema(requiredMode = Schema.RequiredMode.REQUIRED))
    public io.vanguard.testops.project.dto.UserProjectInfoDTO getUserProjectInfoByEmail(@PathVariable String email) {
        return projectUserRoleService.getUserProjectInfoByEmail(email);
    }
}
