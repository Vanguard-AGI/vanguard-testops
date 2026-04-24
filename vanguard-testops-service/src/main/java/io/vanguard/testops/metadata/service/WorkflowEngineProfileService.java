package io.vanguard.testops.metadata.service;

import io.vanguard.testops.metadata.domain.WorkflowEngineProfile;
import io.vanguard.testops.metadata.dto.*;
import io.vanguard.testops.metadata.mapper.WorkflowEngineProfileMapper;
import io.vanguard.testops.sdk.exception.MSException;
import io.vanguard.testops.system.uid.IDGenerator;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 工作流引擎配置服务
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class WorkflowEngineProfileService {

    @Resource
    private WorkflowEngineProfileMapper workflowEngineProfileMapper;

    /**
     * 创建环境配置
     */
    public String create(WorkflowEngineProfileAddRequest request, String userId) {
        long currentTime = System.currentTimeMillis();

        WorkflowEngineProfile profile = new WorkflowEngineProfile();
        profile.setId(IDGenerator.nextStr());
        profile.setProjectId(request.getProjectId());
        profile.setName(request.getName());
        profile.setEngineType(request.getEngineType());
        profile.setEnvCode(request.getEnvCode());
        profile.setRobots(request.getRobots());
        profile.setDataEndpoint(request.getDataEndpoint());
        profile.setVariables(request.getVariables());
        profile.setDomain(request.getDomain());
        profile.setXxljobInfo(request.getXxljobInfo());
        profile.setMqInfo(request.getMqInfo());
        profile.setDubboInfo(request.getDubboInfo());
        profile.setCreateUser(userId);
        profile.setUpdateUser(userId);
        profile.setCreateTime(currentTime);
        profile.setUpdateTime(currentTime);
        // deleted_time 不设置，默认为 NULL（表示未删除）

        workflowEngineProfileMapper.insert(profile);

        return profile.getId();
    }

    /**
     * 更新环境配置
     */
    public void update(WorkflowEngineProfileUpdateRequest request, String userId) {
        WorkflowEngineProfile existing = checkProfileExist(request.getId());

        WorkflowEngineProfile profile = new WorkflowEngineProfile();
        profile.setId(request.getId());
        profile.setName(request.getName());
        if (StringUtils.isNotBlank(request.getEngineType())) {
            profile.setEngineType(request.getEngineType());
        }
        if (StringUtils.isNotBlank(request.getEnvCode())) {
            profile.setEnvCode(request.getEnvCode());
        }
        profile.setRobots(request.getRobots());
        profile.setDataEndpoint(request.getDataEndpoint());
        profile.setVariables(request.getVariables());
        profile.setDomain(request.getDomain());
        profile.setXxljobInfo(request.getXxljobInfo());
        profile.setMqInfo(request.getMqInfo());
        profile.setDubboInfo(request.getDubboInfo());
        profile.setUpdateUser(userId);
        profile.setUpdateTime(System.currentTimeMillis());

        workflowEngineProfileMapper.updateById(profile);
    }

    /**
     * 删除环境配置(逻辑删除，设置 deleted_time)
     */
    public void delete(String id, String userId) {
        WorkflowEngineProfile profile = checkProfileExist(id);

        WorkflowEngineProfile updateProfile = new WorkflowEngineProfile();
        updateProfile.setId(id);
        updateProfile.setDeletedTime(System.currentTimeMillis());

        workflowEngineProfileMapper.updateById(updateProfile);
    }

    /**
     * 获取环境配置详情
     */
    public WorkflowEngineProfileDTO get(String id) {
        WorkflowEngineProfile profile = workflowEngineProfileMapper.selectByIdWithTimestamp(id);
        if (profile == null) {
            throw new MSException("环境配置不存在");
        }

        WorkflowEngineProfileDTO dto = new WorkflowEngineProfileDTO();
        dto.setId(profile.getId());
        dto.setProjectId(profile.getProjectId());
        dto.setName(profile.getName());
        dto.setEngineType(profile.getEngineType());
        dto.setEnvCode(profile.getEnvCode());
        dto.setRobots(profile.getRobots());
        dto.setDataEndpoint(profile.getDataEndpoint());
        dto.setVariables(profile.getVariables());
        dto.setDomain(profile.getDomain());
        dto.setXxljobInfo(profile.getXxljobInfo());
        dto.setMqInfo(profile.getMqInfo());
        dto.setDubboInfo(profile.getDubboInfo());
        dto.setCreateUser(profile.getCreateUser());
        dto.setUpdateUser(profile.getUpdateUser());
        dto.setCreateTime(profile.getCreateTime());
        dto.setUpdateTime(profile.getUpdateTime());

        return dto;
    }

    /**
     * 分页查询环境配置列表
     */
    public List<WorkflowEngineProfileDTO> list(WorkflowEngineProfilePageRequest request) {
        List<WorkflowEngineProfile> profiles = workflowEngineProfileMapper.selectByProjectId(request.getProjectId());

        // 过滤条件
        if (StringUtils.isNotBlank(request.getEngineType())) {
            profiles = profiles.stream()
                    .filter(p -> StringUtils.equalsIgnoreCase(p.getEngineType(), request.getEngineType()))
                    .collect(Collectors.toList());
        }
        if (StringUtils.isNotBlank(request.getEnvCode())) {
            profiles = profiles.stream()
                    .filter(p -> StringUtils.equalsIgnoreCase(p.getEnvCode(), request.getEnvCode()))
                    .collect(Collectors.toList());
        }
        if (StringUtils.isNotBlank(request.getKeyword())) {
            String keyword = request.getKeyword().toLowerCase();
            profiles = profiles.stream()
                    .filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(keyword)))
                    .collect(Collectors.toList());
        }

        // 转换为 DTO
        return profiles.stream().map(profile -> {
            WorkflowEngineProfileDTO dto = new WorkflowEngineProfileDTO();
            dto.setId(profile.getId());
            dto.setProjectId(profile.getProjectId());
            dto.setName(profile.getName());
            dto.setEngineType(profile.getEngineType());
            dto.setEnvCode(profile.getEnvCode());
            dto.setRobots(profile.getRobots());
            dto.setDataEndpoint(profile.getDataEndpoint());
            dto.setVariables(profile.getVariables());
            dto.setDomain(profile.getDomain());
            dto.setXxljobInfo(profile.getXxljobInfo());
            dto.setMqInfo(profile.getMqInfo());
            dto.setDubboInfo(profile.getDubboInfo());
            dto.setCreateUser(profile.getCreateUser());
            dto.setUpdateUser(profile.getUpdateUser());
            dto.setCreateTime(profile.getCreateTime());
            dto.setUpdateTime(profile.getUpdateTime());
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 检查环境配置是否存在
     * 注意：selectByIdWithTimestamp 已经过滤了已删除的记录（deleted_time IS NULL）
     */
    private WorkflowEngineProfile checkProfileExist(String profileId) {
        WorkflowEngineProfile profile = workflowEngineProfileMapper.selectByIdWithTimestamp(profileId);
        if (profile == null) {
            throw new MSException("环境配置不存在");
        }
        return profile;
    }
}

