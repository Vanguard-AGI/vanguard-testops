package io.vanguard.testops.project.service;

import io.vanguard.testops.project.domain.Project;
import io.vanguard.testops.project.dto.environment.EnvironmentConfig;
import io.vanguard.testops.project.dto.environment.EnvironmentRequest;
import io.vanguard.testops.project.mapper.ProjectMapper;
import io.vanguard.testops.sdk.domain.Environment;
import io.vanguard.testops.sdk.domain.EnvironmentBlob;
import io.vanguard.testops.system.log.dto.LogDTO;

import io.vanguard.testops.sdk.mapper.EnvironmentBlobMapper;
import io.vanguard.testops.sdk.mapper.EnvironmentMapper;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.system.log.constants.OperationLogModule;
import io.vanguard.testops.system.log.constants.OperationLogType;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional(rollbackFor = Exception.class)
public class EnvironmentLogService {


    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private EnvironmentMapper environmentMapper;
    @Resource
    private EnvironmentBlobMapper environmentBlobMapper;

    public LogDTO addLog(EnvironmentRequest request) {
        Project project = getProject(request.getProjectId());
        LogDTO dto = new LogDTO(
                request.getProjectId(),
                project.getOrganizationId(),
                request.getId(),
                null,
                OperationLogType.ADD.name(),
                OperationLogModule.PROJECT_MANAGEMENT_ENVIRONMENT,
                request.getName());
        dto.setOriginalValue(JSON.toJSONBytes(request));
        return dto;
    }


    public LogDTO updateLog(EnvironmentRequest request) {
        Project project = getProject(request.getProjectId());
        LogDTO dto = new LogDTO(
                project.getId(),
                project.getOrganizationId(),
                request.getId(),
                null,
                OperationLogType.UPDATE.name(),
                OperationLogModule.PROJECT_MANAGEMENT_ENVIRONMENT,
                request.getName());
        Environment environment = environmentMapper.selectByPrimaryKey(request.getId());
        EnvironmentBlob environmentBlob = environmentBlobMapper.selectByPrimaryKey(request.getId());
        EnvironmentRequest before = new EnvironmentRequest();
        before.setName(environment.getName());
        before.setConfig(JSON.parseObject(new String(environmentBlob.getConfig()), EnvironmentConfig.class));
        dto.setOriginalValue(JSON.toJSONBytes(before));
        dto.setModifiedValue(JSON.toJSONBytes(request));
        return dto;
    }

    public LogDTO deleteLog(String id) {
        Environment environment = environmentMapper.selectByPrimaryKey(id);
        Project project = getProject(environment.getProjectId());
        return new LogDTO(
                project.getId(),
                project.getOrganizationId(),
                id,
                null,
                OperationLogType.DELETE.name(),
                OperationLogModule.PROJECT_MANAGEMENT_ENVIRONMENT,
                environment.getName());
    }


    private Project getProject(String id) {
        return projectMapper.selectByPrimaryKey(id);
    }
}
