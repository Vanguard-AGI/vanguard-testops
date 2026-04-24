package io.vanguard.testops.project.service;

import com.alibaba.excel.util.StringUtils;
import io.vanguard.testops.project.domain.Project;
import io.vanguard.testops.project.domain.ProjectExample;
import io.vanguard.testops.project.dto.ProjectRequest;
import io.vanguard.testops.project.dto.ProjectUserRoleDTO;
import io.vanguard.testops.project.dto.UserProjectInfoDTO;
import io.vanguard.testops.project.mapper.ExtProjectUserRoleMapper;
import io.vanguard.testops.project.mapper.ProjectMapper;
import io.vanguard.testops.project.request.ProjectUserRoleMemberEditRequest;
import io.vanguard.testops.project.request.ProjectUserRoleMemberRequest;
import io.vanguard.testops.project.request.ProjectUserRoleRequest;
import io.vanguard.testops.sdk.constants.InternalUserRole;
import io.vanguard.testops.sdk.constants.UserRoleType;
import io.vanguard.testops.sdk.exception.MSException;
import io.vanguard.testops.sdk.util.Translator;
import io.vanguard.testops.system.domain.User;
import io.vanguard.testops.system.domain.UserExample;
import io.vanguard.testops.system.domain.UserRole;
import io.vanguard.testops.system.domain.UserRoleRelation;
import io.vanguard.testops.system.domain.UserRoleRelationExample;
import io.vanguard.testops.system.dto.permission.PermissionDefinitionItem;
import io.vanguard.testops.system.dto.sdk.request.PermissionSettingUpdateRequest;
import io.vanguard.testops.system.mapper.UserMapper;
import io.vanguard.testops.system.mapper.UserRoleMapper;
import io.vanguard.testops.system.mapper.UserRolePermissionMapper;
import io.vanguard.testops.system.mapper.UserRoleRelationMapper;
import io.vanguard.testops.system.service.BaseUserRoleService;
import io.vanguard.testops.system.uid.IDGenerator;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.vanguard.testops.system.controller.result.SystemResultCode.NO_PROJECT_USER_ROLE_PERMISSION;

/**
 * 项目-用户组与权限
 *
 * @author Jan
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ProjectUserRoleService extends BaseUserRoleService {

    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private UserRoleMapper userRoleMapper;
    @Resource
    private UserRoleRelationMapper userRoleRelationMapper;
    @Resource
    private ExtProjectUserRoleMapper extProjectUserRoleMapper;
	@Autowired
	private UserRolePermissionMapper userRolePermissionMapper;
	@Resource
	private UserMapper userMapper;

    public List<ProjectUserRoleDTO> list(ProjectUserRoleRequest request) {
        List<ProjectUserRoleDTO> roles = extProjectUserRoleMapper.list(request);
        if (CollectionUtils.isEmpty(roles)) {
            return new ArrayList<>();
        }
        List<String> roleIds = roles.stream().map(ProjectUserRoleDTO::getId).toList();
        List<UserRoleRelation> relations = extProjectUserRoleMapper.getRelationByRoleIds(request.getProjectId(), roleIds);
        if (CollectionUtils.isNotEmpty(relations)) {
            Map<String, Long> countMap = relations.stream().collect(Collectors.groupingBy(UserRoleRelation::getRoleId, Collectors.counting()));
            roles.forEach(role -> {
                if (countMap.containsKey(role.getId())) {
                    role.setMemberCount(countMap.get(role.getId()).intValue());
                } else {
                    role.setMemberCount(0);
                }
            });
        } else {
            roles.forEach(role -> {
                role.setMemberCount(0);
            });
        }
        return roles;
    }

    @Override
    public UserRole add(UserRole userRole) {
        userRole.setInternal(false);
        userRole.setType(UserRoleType.PROJECT.name());
        checkNewRoleExist(userRole);
        return super.add(userRole);
    }

    @Override
    public UserRole update(UserRole userRole) {
        UserRole oldRole = get(userRole.getId());
        // 非项目用户组, 全局用户组不允许修改
        checkProjectUserRole(oldRole);
        checkGlobalUserRole(oldRole);
        userRole.setType(UserRoleType.PROJECT.name());
        checkNewRoleExist(userRole);
        return super.update(userRole);
    }

    public void delete(String roleId, String currentUserId) {
        UserRole userRole = get(roleId);
        // 非项目用户组不允许删除, 内置用户组不允许删除
        checkProjectUserRole(userRole);
        checkGlobalUserRole(userRole);
        super.delete(userRole, InternalUserRole.PROJECT_MEMBER.getValue(), currentUserId, userRole.getScopeId());
    }

    public List<User> listMember(ProjectUserRoleMemberRequest request) {
        return extProjectUserRoleMapper.listProjectRoleMember(request);
    }

    public void addMember(ProjectUserRoleMemberEditRequest request, String createUserId) {
        Project project = projectMapper.selectByPrimaryKey(request.getProjectId());
        request.getUserIds().forEach(userId -> {
            checkMemberParam(userId, request.getUserRoleId());
            UserRoleRelation relation = new UserRoleRelation();
            relation.setId(IDGenerator.nextStr());
            relation.setUserId(userId);
            relation.setRoleId(request.getUserRoleId());
            relation.setSourceId(request.getProjectId());
            relation.setCreateTime(System.currentTimeMillis());
            relation.setCreateUser(createUserId);
            relation.setOrganizationId(project.getOrganizationId());
            userRoleRelationMapper.insert(relation);
        });
    }

    public void removeMember(ProjectUserRoleMemberEditRequest request) {
        String removeUserId = request.getUserIds().getFirst();
        checkMemberParam(removeUserId, request.getUserRoleId());
        // 检查移除的是不是管理员
        if (StringUtils.equals(request.getUserRoleId(), InternalUserRole.PROJECT_ADMIN.getValue())) {
            UserRoleRelationExample userRoleRelationExample = new UserRoleRelationExample();
            userRoleRelationExample.createCriteria().andUserIdNotEqualTo(removeUserId)
                    .andSourceIdEqualTo(request.getProjectId())
                    .andRoleIdEqualTo(InternalUserRole.PROJECT_ADMIN.getValue());
            if (userRoleRelationMapper.countByExample(userRoleRelationExample) == 0) {
                throw new MSException(Translator.get("keep_at_least_one_administrator"));
            }
        }
        // 移除项目-用户组的成员, 若成员只存在该项目下唯一用户组, 则提示不能移除
        UserRoleRelationExample example = new UserRoleRelationExample();
        example.createCriteria().andUserIdEqualTo(removeUserId)
                .andRoleIdNotEqualTo(request.getUserRoleId())
                .andSourceIdEqualTo(request.getProjectId());
        if (userRoleRelationMapper.countByExample(example) == 0) {
            throw new MSException(Translator.get("project_at_least_one_user_role_require"));
        }
        example.clear();
        example.createCriteria().andUserIdEqualTo(removeUserId)
                .andRoleIdEqualTo(request.getUserRoleId())
                .andSourceIdEqualTo(request.getProjectId());
        userRoleRelationMapper.deleteByExample(example);
    }

    public List<PermissionDefinitionItem> getPermissionSetting(String id) {
        UserRole userRole = get(id);
        checkProjectUserRole(userRole);
        return getPermissionSetting(userRole);
    }

    @Override
    public void updatePermissionSetting(PermissionSettingUpdateRequest request) {
        UserRole userRole = get(request.getUserRoleId());
        checkProjectUserRole(userRole);
        checkGlobalUserRole(userRole);
        super.updatePermissionSetting(request);
    }

    @Override
    public UserRole get(String id) {
        UserRole userRole = userRoleMapper.selectByPrimaryKey(id);
        if (userRole == null) {
            throw new MSException(Translator.get("user_role_not_exist"));
        }
        return userRole;
    }

    /**
     * 校验是否项目下用户组
     *
     * @param userRole 用户组
     */
    private void checkProjectUserRole(UserRole userRole) {
        if (!UserRoleType.PROJECT.name().equals(userRole.getType())) {
            throw new MSException(NO_PROJECT_USER_ROLE_PERMISSION);
        }
    }

    /**
     * 通过邮箱查询用户ID和项目ID列表
     *
     * @param email 用户邮箱
     * @return 用户项目信息
     */
    public UserProjectInfoDTO getUserProjectInfoByEmail(String email) {
        UserExample userExample = new UserExample();
        userExample.createCriteria().andEmailEqualTo(email).andDeletedEqualTo(false);
        List<User> users = userMapper.selectByExample(userExample);
        
        if (CollectionUtils.isEmpty(users)) {
            throw new MSException("用户不存在: " + email);
        }
        
        User user = users.get(0);
        String userId = user.getId();
        
        UserRoleRelationExample relationExample = new UserRoleRelationExample();
        relationExample.createCriteria().andUserIdEqualTo(userId);
        List<UserRoleRelation> relations = userRoleRelationMapper.selectByExample(relationExample);
        
        List<String> projectIds = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(relations)) {
            projectIds = relations.stream()
                    .map(UserRoleRelation::getSourceId)
                    .distinct()
                    .collect(Collectors.toList());
        }
        
        List<ProjectRequest> projectRequests = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(projectIds)) {
            ProjectExample projectExample = new ProjectExample();
            projectExample.createCriteria().andIdIn(projectIds).andDeletedEqualTo(false);
            List<Project> projects = projectMapper.selectByExample(projectExample);
            
            if (CollectionUtils.isNotEmpty(projects)) {
                projectRequests = projects.stream().map(project -> {
                    ProjectRequest projectRequest = new ProjectRequest();
                    projectRequest.setId(project.getId());
                    projectRequest.setOrganizationId(project.getOrganizationId());
                    projectRequest.setName(project.getName());
                    projectRequest.setDescription(project.getDescription());
                    projectRequest.setEnable(project.getEnable());
                    return projectRequest;
                }).collect(Collectors.toList());
            }
        }
        
        UserProjectInfoDTO result = new UserProjectInfoDTO();
        result.setUserId(userId);
        result.setProjects(projectRequests);
        
        return result;
    }

    /** 示例项目 ID，选「最早项目」时排除 */
    private static final String EXAMPLE_PROJECT_ID = "100001100001";

    /**
     * 根据用户邮箱获取其最早加入的项目 ID（用于埋点落库缺省项目），排除示例项目 100001100001。
     *
     * @param email 用户邮箱
     * @return 最早的项目 ID，若无则 null
     */
    public String getEarliestProjectIdByEmail(String email) {
        if (StringUtils.isBlank(email)) {
            return null;
        }
        UserExample userExample = new UserExample();
        userExample.createCriteria().andEmailEqualTo(email).andDeletedEqualTo(false);
        List<User> users = userMapper.selectByExample(userExample);
        if (CollectionUtils.isEmpty(users)) {
            return null;
        }
        String userId = users.get(0).getId();
        UserRoleRelationExample relationExample = new UserRoleRelationExample();
        relationExample.createCriteria().andUserIdEqualTo(userId);
        List<UserRoleRelation> relations = userRoleRelationMapper.selectByExample(relationExample);
        if (CollectionUtils.isEmpty(relations)) {
            return null;
        }
        List<String> projectIds = relations.stream()
                .map(UserRoleRelation::getSourceId)
                .distinct()
                .filter(id -> !EXAMPLE_PROJECT_ID.equals(id))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(projectIds)) {
            return null;
        }
        ProjectExample projectExample = new ProjectExample();
        projectExample.createCriteria().andIdIn(projectIds).andDeletedEqualTo(false);
        projectExample.setOrderByClause("create_time ASC");
        List<Project> projects = projectMapper.selectByExample(projectExample);
        return projects.isEmpty() ? null : projects.get(0).getId();
    }
}
