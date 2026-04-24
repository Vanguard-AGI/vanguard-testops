package io.vanguard.testops.functional.service;

import io.vanguard.testops.functional.domain.FunctionalCase;
import io.vanguard.testops.functional.domain.FunctionalCaseModule;
import io.vanguard.testops.project.domain.Project;
import io.vanguard.testops.project.mapper.ProjectMapper;
import io.vanguard.testops.sdk.constants.HttpMethodConstants;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.system.dto.builder.LogDTOBuilder;
import io.vanguard.testops.system.log.constants.OperationLogModule;
import io.vanguard.testops.system.log.constants.OperationLogType;
import io.vanguard.testops.system.log.dto.LogDTO;
import io.vanguard.testops.system.log.service.OperationLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class FunctionalCaseModuleLogService {
    private static final String FUNCTIONAL_CASE_MODULE = "/functional/case/module";
    private static final String ADD = FUNCTIONAL_CASE_MODULE + "/add";
    private static final String UPDATE = FUNCTIONAL_CASE_MODULE + "/update";
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private OperationLogService operationLogService;

    /**
     * 功能用例添加模块日志
     *
     * @param functionalCaseModule
     * @param userId
     */
    public void addModuleLog(FunctionalCaseModule functionalCaseModule, String userId) {
        Project project = projectMapper.selectByPrimaryKey(functionalCaseModule.getProjectId());
        LogDTO dto = LogDTOBuilder.builder()
                .projectId(functionalCaseModule.getProjectId())
                .organizationId(project.getOrganizationId())
                .type(OperationLogType.ADD.name())
                .module(OperationLogModule.CASE_MANAGEMENT_CASE_MODULE)
                .method(HttpMethodConstants.POST.name())
                .path(ADD)
                .sourceId(functionalCaseModule.getId())
                .content(functionalCaseModule.getName())
                .originalValue(JSON.toJSONBytes(functionalCaseModule))
                .createUser(userId)
                .build().getLogDTO();
        operationLogService.add(dto);
    }


    /**
     * 功能用例更新模块日志
     *
     * @param module
     * @param operator
     */
    public void updateModuleLog(FunctionalCaseModule module, String operator) {
        Project project = projectMapper.selectByPrimaryKey(module.getProjectId());
        LogDTO dto = LogDTOBuilder.builder()
                .projectId(project.getId())
                .organizationId(project.getOrganizationId())
                .type(OperationLogType.UPDATE.name())
                .module(OperationLogModule.CASE_MANAGEMENT_CASE_MODULE)
                .method(HttpMethodConstants.POST.name())
                .path(UPDATE)
                .sourceId(module.getId())
                .content(module.getName())
                .originalValue(JSON.toJSONBytes(module))
                .createUser(operator)
                .build().getLogDTO();
        operationLogService.add(dto);
    }


    /**
     * 功能用例用例删除日志
     *
     * @param functionalCases
     * @param projectId
     */
    public void batchDelLog(List<FunctionalCase> functionalCases, String projectId, String userId, String path) {
        Project project = projectMapper.selectByPrimaryKey(projectId);
        List<LogDTO> dtoList = new ArrayList<>();
        functionalCases.forEach(item -> {
            LogDTO dto = new LogDTO(
                    projectId,
                    project.getOrganizationId(),
                    item.getId(),
                    userId,
                    OperationLogType.DELETE.name(),
                    OperationLogModule.CASE_MANAGEMENT_CASE_CASE,
                    item.getName());
            dto.setPath(path);
            dto.setMethod(HttpMethodConstants.GET.name());
            dto.setOriginalValue(JSON.toJSONBytes(item));
            dtoList.add(dto);
        });
        operationLogService.batchAdd(dtoList);
    }


    /**
     * 功能用例模块删除日志
     *
     * @param deleteModule
     * @param projectId
     * @param userId
     * @param path
     */
    public void handleModuleLog(List<FunctionalCaseModule> deleteModule, String projectId, String userId, String path, String type, String deleteDesc) {
        Project project = projectMapper.selectByPrimaryKey(projectId);
        List<LogDTO> dtoList = new ArrayList<>();
        deleteModule.forEach(item -> {
            LogDTO dto = new LogDTO(
                    projectId,
                    project.getOrganizationId(),
                    item.getId(),
                    userId,
                    type,
                    OperationLogModule.CASE_MANAGEMENT_CASE_MODULE,
                    item.getName() + deleteDesc);
            dto.setPath(path);
            dto.setMethod(HttpMethodConstants.GET.name());
            dto.setOriginalValue(JSON.toJSONBytes(item));
            dtoList.add(dto);
        });
        operationLogService.batchAdd(dtoList);
    }
}
