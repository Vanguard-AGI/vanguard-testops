package io.vanguard.testops.api.mapper;

import io.vanguard.testops.api.domain.ApiScenarioBlob;
import io.vanguard.testops.api.domain.ApiScenarioReport;
import io.vanguard.testops.api.dto.ApiExecResultDTO;
import io.vanguard.testops.api.dto.definition.ApiReportBatchRequest;
import io.vanguard.testops.api.dto.definition.ApiReportPageRequest;
import io.vanguard.testops.api.dto.definition.ExecuteReportDTO;
import io.vanguard.testops.api.dto.report.ReportDTO;
import io.vanguard.testops.api.dto.scenario.ApiScenarioReportStepDTO;
import io.vanguard.testops.system.dto.sdk.ApiReportMessageDTO;
import io.vanguard.testops.system.dto.taskcenter.TaskCenterDTO;
import io.vanguard.testops.system.dto.taskcenter.request.TaskCenterBatchRequest;
import io.vanguard.testops.system.dto.taskcenter.request.TaskCenterPageRequest;
import io.vanguard.testops.system.interceptor.BaseConditionFilter;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtApiScenarioReportMapper {
    @BaseConditionFilter
    List<ApiScenarioReport> list(@Param("request") ApiReportPageRequest request);

    @BaseConditionFilter
    List<String> getIds(@Param("request") ApiReportBatchRequest request);

    List<ApiScenarioReport> selectApiReportByIds(@Param("ids") List<String> ids);

    List<ApiScenarioReportStepDTO> selectStepByReportId(@Param("reportId") String reportId);

    List<String> selectApiScenarioReportByProjectId(String projectId);


    int countScenarioReportByTime(@Param("time") long time, @Param("projectId") String projectId);

    List<String> selectApiReportByProjectIdAndTime(@Param("time") long time, @Param("projectId") String projectId);

    List<TaskCenterDTO> taskCenterlist(@Param("request") TaskCenterPageRequest request, @Param("projectIds") List<String> projectIds,
                                       @Param("startTime") long startTime, @Param("endTime") long endTime);


    List<ReportDTO> getReports(@Param("request") TaskCenterBatchRequest request, @Param("projectIds") List<String> projectIds,
                               @Param("ids") List<String> ids, @Param("startTime") long startTime, @Param("endTime") long endTime);

    void updateReportStatus(@Param("ids") List<String> ids, @Param("time") long time, @Param("userId") String userId);

    List<ReportDTO> selectByIds(@Param("ids") List<String> ids);

    /**
     * 根据项目获取组织id
     */
    List<ReportDTO> getOrgListByProjectIds(@Param("ids") List<String> ids);

    ApiScenarioBlob getScenarioBlob(String id);

    void updateApiScenario(List<String> ids);

    List<ApiScenarioReportStepDTO> selectStepDetailByReportId(@Param("id") String id);

    List<ApiReportMessageDTO> getNoticeList(@Param("ids") List<String> ids);


    List<ExecuteReportDTO> getHistoryDeleted(@Param("ids") List<String> ids);

    List<ExecuteReportDTO> getTestPlanNum(@Param("ids") List<String> ids);

    List<ApiExecResultDTO> selectExecResultByScenarioIds(@Param("ids") List<String> scenarioIds);
}
