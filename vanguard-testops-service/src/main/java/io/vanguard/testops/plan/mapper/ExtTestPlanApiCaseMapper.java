package io.vanguard.testops.plan.mapper;

import io.vanguard.testops.api.dto.definition.ApiDefinitionDTO;
import io.vanguard.testops.functional.dto.FunctionalCaseModuleCountDTO;
import io.vanguard.testops.functional.dto.ProjectOptionDTO;
import io.vanguard.testops.plan.domain.TestPlanApiCase;
import io.vanguard.testops.plan.dto.*;
import io.vanguard.testops.plan.dto.request.TestPlanApiCaseBatchRequest;
import io.vanguard.testops.plan.dto.request.TestPlanApiCaseModuleRequest;
import io.vanguard.testops.plan.dto.request.TestPlanApiCaseRequest;
import io.vanguard.testops.plan.dto.request.TestPlanApiRequest;
import io.vanguard.testops.plan.dto.response.TestPlanApiCasePageResponse;
import io.vanguard.testops.project.dto.DropNode;
import io.vanguard.testops.project.dto.ModuleCountDTO;
import io.vanguard.testops.project.dto.NodeSortQueryParam;
import io.vanguard.testops.system.interceptor.BaseConditionFilter;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtTestPlanApiCaseMapper {

    long updatePos(@Param("id") String id, @Param("pos") long pos);

    List<String> selectIdByTestPlanIdOrderByPos(String testPlanId);

    Long getMaxPosByTestPlanId(String testPlanId);

    List<String> getIdByParam(ResourceSelectParam resourceSelectParam);

    DropNode selectDragInfoById(String id);

    DropNode selectNodeByPosOperator(NodeSortQueryParam nodeSortQueryParam);

    List<TestPlanCaseRunResultCount> selectCaseExecResultCount(String testPlanId);

    @BaseConditionFilter
    List<ApiDefinitionDTO> list(@Param("request") TestPlanApiRequest request, @Param("isRepeat") boolean isRepeat);

    @BaseConditionFilter
    List<TestPlanApiCasePageResponse> relateApiCaseList(@Param("request") TestPlanApiCaseRequest request, @Param("deleted") boolean deleted);

    List<FunctionalCaseModuleCountDTO> countModuleIdByRequest(@Param("request") TestPlanApiCaseRequest request, @Param("deleted") boolean deleted);

    List<String> selectIdByProjectIdAndTestPlanId(@Param("projectId") String projectId, @Param("testPlanId") String testPlanId);

    long caseCount(@Param("request") TestPlanApiCaseRequest request, @Param("deleted") boolean deleted);

    List<TestPlanApiCase> selectByTestPlanIdAndNotDeleted(String testPlanId);

    List<ProjectOptionDTO> selectRootIdByTestPlanId(@Param("testPlanId") String testPlanId);

    List<ApiCaseModuleDTO> selectBaseByProjectIdAndTestPlanId(@Param("testPlanId") String testPlanId);

    @BaseConditionFilter
    List<String> getIds(@Param("request") TestPlanApiCaseBatchRequest request, @Param("deleted") boolean deleted);

    void batchUpdateExecutor(@Param("ids") List<String> ids, @Param("userId") String userId);

    List<ModuleCountDTO> collectionCountByRequest(@Param("request") TestPlanApiCaseModuleRequest request);

    Long getMaxPosByCollectionId(String collectionId);

    /**
     * 获取计划下的功能用例集合
     *
     * @param planIds 测试计划ID集合
     * @return 计划功能用例集合
     */
    List<TestPlanApiCase> getPlanApiCaseByIds(@Param("planIds") List<String> planIds);

    List<TestPlanApiCase> getApiCaseExecuteInfoByIds(@Param("ids") List<String> ids);
    @BaseConditionFilter
    List<TestPlanApiCaseBatchRunDTO> getSelectIdAndCollectionId(@Param("request") TestPlanApiCaseBatchRequest request);

    List<TestPlanApiCaseBatchRunDTO> getBatchRunInfoByIds(@Param("ids") List<String> ids);

    List<TestPlanApiCase> getPlanApiCaseNotDeletedByCollectionIds(@Param("collectionIds") List<String> collectionIds);

    List<TestPlanResourceExecResultDTO> selectDistinctExecResult(String projectId);

    List<TestPlanResourceExecResultDTO> selectDistinctExecResultByTestPlanIds(@Param("testPlanIds") List<String> testPlanIds);

    Integer countByPlanIds(@Param("planIds") List<String> planIds);

    List<TestPlanResourceExecResultDTO> selectLastExecResultByTestPlanIds(@Param("testPlanIds") List<String> testPlanIds);

    List<TestPlanResourceExecResultDTO> selectLastExecResultByProjectId(String projectId);
}
