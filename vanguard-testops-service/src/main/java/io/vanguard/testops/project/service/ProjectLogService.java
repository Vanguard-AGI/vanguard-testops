package io.vanguard.testops.project.service;

import io.vanguard.testops.project.domain.Project;
import io.vanguard.testops.project.dto.ProjectRequest;
import io.vanguard.testops.project.mapper.ProjectMapper;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.system.log.constants.OperationLogModule;
import io.vanguard.testops.system.log.constants.OperationLogType;
import io.vanguard.testops.system.log.dto.LogDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class ProjectLogService {

    @Resource
    private ProjectMapper projectMapper;

    /**
     * @param request
     * @return
     */
    public LogDTO updateLog(ProjectRequest request) {
        Project project = projectMapper.selectByPrimaryKey(request.getId());
        if (project != null) {
            LogDTO dto = new LogDTO(
                    project.getId(),
                    project.getOrganizationId(),
                    project.getId(),
                    project.getCreateUser(),
                    OperationLogType.UPDATE.name(),
                    OperationLogModule.PROJECT_MANAGEMENT_PERMISSION_BASIC_INFO,
                    request.getName());

            dto.setOriginalValue(JSON.toJSONBytes(project));
            return dto;
        }
        return null;
    }
}
