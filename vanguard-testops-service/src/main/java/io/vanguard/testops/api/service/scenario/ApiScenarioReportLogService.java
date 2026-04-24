package io.vanguard.testops.api.service.scenario;

import io.vanguard.testops.api.domain.ApiScenarioReport;
import io.vanguard.testops.api.mapper.ApiScenarioReportMapper;
import io.vanguard.testops.api.mapper.ExtApiScenarioReportMapper;
import io.vanguard.testops.project.domain.Project;
import io.vanguard.testops.project.mapper.ProjectMapper;
import io.vanguard.testops.sdk.constants.HttpMethodConstants;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.system.log.constants.OperationLogModule;
import io.vanguard.testops.system.log.constants.OperationLogType;
import io.vanguard.testops.system.log.dto.LogDTO;
import io.vanguard.testops.system.log.service.OperationLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ApiScenarioReportLogService {

    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private OperationLogService operationLogService;
    @Resource
    private ExtApiScenarioReportMapper extApiScenarioReportMapper;
    @Resource
    private ApiScenarioReportMapper apiScenarioReportMapper;


    public LogDTO deleteLog(String id) {
        ApiScenarioReport scenarioReport = apiScenarioReportMapper.selectByPrimaryKey(id);
        Project project = projectMapper.selectByPrimaryKey(scenarioReport.getProjectId());
        LogDTO dto = new LogDTO(
                scenarioReport.getProjectId(),
                project.getOrganizationId(),
                scenarioReport.getId(),
                null,
                OperationLogType.DELETE.name(),
                OperationLogModule.API_TEST_REPORT_SCENARIO,
                scenarioReport.getName());

        dto.setPath("/api/report/scenario/delete/" + scenarioReport.getId());
        dto.setMethod(HttpMethodConstants.GET.name());
        dto.setOriginalValue(JSON.toJSONBytes(scenarioReport));
        return dto;
    }

    public LogDTO updateLog(String id) {
        ApiScenarioReport scenarioReport = apiScenarioReportMapper.selectByPrimaryKey(id);
        Project project = projectMapper.selectByPrimaryKey(scenarioReport.getProjectId());
        LogDTO dto = new LogDTO(
                scenarioReport.getProjectId(),
                project.getOrganizationId(),
                scenarioReport.getId(),
                null,
                OperationLogType.UPDATE.name(),
                OperationLogModule.API_TEST_REPORT_SCENARIO,
                scenarioReport.getName());

        dto.setPath("/api/report/scenario/rename/" + scenarioReport.getId() + "/" + scenarioReport.getName());
        dto.setMethod(HttpMethodConstants.GET.name());
        dto.setOriginalValue(JSON.toJSONBytes(scenarioReport));
        return dto;
    }

    public void batchDeleteLog(List<String> ids, String userId, String projectId) {
        Project project = projectMapper.selectByPrimaryKey(projectId);
        List<ApiScenarioReport> apiReports = extApiScenarioReportMapper.selectApiReportByIds(ids);
        List<LogDTO> logs = new ArrayList<>();
        apiReports.forEach(apiReport -> {
            LogDTO dto = new LogDTO(
                    projectId,
                    project.getOrganizationId(),
                    apiReport.getId(),
                    userId,
                    OperationLogType.DELETE.name(),
                    OperationLogModule.API_TEST_REPORT_SCENARIO,
                    apiReport.getName());

            dto.setPath("/api/report/scenario/batch/delete");
            dto.setMethod(HttpMethodConstants.POST.name());
            dto.setOriginalValue(JSON.toJSONBytes(apiReport));
            logs.add(dto);
        });
        operationLogService.batchAdd(logs);
    }

    public void exportLog(List<ApiScenarioReport> reports, String userId, String projectId, String path) {
        Project project = projectMapper.selectByPrimaryKey(projectId);
        List<LogDTO> logs = new ArrayList<>();
        reports.forEach(report -> {
            LogDTO dto = new LogDTO(
                    projectId,
                    project.getOrganizationId(),
                    report.getId(),
                    userId,
                    OperationLogType.EXPORT.name(),
                    OperationLogModule.API_TEST_REPORT_SCENARIO,
                    report.getName());
            dto.setPath(path);
            dto.setMethod(HttpMethodConstants.POST.name());
            logs.add(dto);
        });
        operationLogService.batchAdd(logs);
    }
}
