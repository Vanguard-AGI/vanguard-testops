package io.vanguard.testops.plan.mapper;

import io.vanguard.testops.functional.domain.FunctionalCaseModule;
import io.vanguard.testops.functional.dto.FunctionalCaseModuleCountDTO;
import io.vanguard.testops.functional.dto.FunctionalCaseModuleDTO;
import io.vanguard.testops.functional.dto.ProjectOptionDTO;
import io.vanguard.testops.plan.domain.TestPlanFunctionalCase;
import io.vanguard.testops.plan.dto.ResourceSelectParam;
import io.vanguard.testops.plan.dto.TestPlanCaseRunResultCount;
import io.vanguard.testops.plan.dto.TestPlanResourceExecResultDTO;
import io.vanguard.testops.plan.dto.request.BasePlanCaseBatchRequest;
import io.vanguard.testops.plan.dto.request.TestPlanCaseMinderRequest;
import io.vanguard.testops.plan.dto.request.TestPlanCaseModuleRequest;
import io.vanguard.testops.plan.dto.request.TestPlanCaseRequest;
import io.vanguard.testops.plan.dto.response.TestPlanCasePageResponse;
import io.vanguard.testops.project.dto.DropNode;
import io.vanguard.testops.project.dto.ModuleCountDTO;
import io.vanguard.testops.project.dto.NodeSortQueryParam;
import io.vanguard.testops.system.interceptor.BaseConditionFilter;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface ExtTestPlanFunctionalCaseMapper {

    long updatePos(@Param("id") String id, @Param("pos") long pos);

    List<String> selectIdByTestPlanIdOrderByPos(String testPlanId);

    List<String> selectIdByTestPlanIds(@Param("testPlanIds") List<String> testPlanIdList);

    Long getMaxPosByTestPlanId(String testPlanId);

    List<String> getIdByParam(ResourceSelectParam resourceSelectParam);

    DropNode selectDragInfoById(String id);

    DropNode selectNodeByPosOperator(NodeSortQueryParam nodeSortQueryParam);

    @BaseConditionFilter
    List<TestPlanCasePageResponse> getCasePage(@Param("request") TestPlanCaseRequest request, @Param("deleted") boolean deleted, @Param("sort") String sort);

    List<TestPlanFunctionalCase> selectByTestPlanIdAndNotDeleted(@Param("testPlanId") String testPlanId);

    List<ProjectOptionDTO> selectRootIdByTestPlanId(@Param("testPlanId") String testPlanId);

    List<FunctionalCaseModuleDTO> selectBaseByProjectIdAndTestPlanId(@Param("testPlanId") String testPlanId);

    List<FunctionalCaseModuleCountDTO> countModuleIdByRequest(@Param("request") TestPlanCaseRequest request, @Param("deleted") boolean deleted);

    long caseCount(@Param("request") TestPlanCaseRequest request, @Param("deleted") boolean deleted);

    @BaseConditionFilter
    List<String> getIds(@Param("request") BasePlanCaseBatchRequest request, @Param("deleted") boolean deleted);

    /**
     * 获取计划下的功能用例集合
     *
     * @param planIds 测试计划ID集合
     * @return 计划功能用例集合
     */
    List<TestPlanFunctionalCase> getPlanFunctionalCaseByIds(@Param("planIds") List<String> planIds);

    void batchUpdate(@Param("ids") List<String> ids, @Param("lastExecResult") String lastExecResult, @Param("lastExecTime") long lastExecTime, @Param("userId") String userId);

    void batchUpdateExecutor(@Param("ids") List<String> ids, @Param("userId") String userId);

    List<TestPlanCaseRunResultCount> selectCaseExecResultCount(String testPlanId);

    Long getMaxPosByCollectionId(String collectionId);

    List<ModuleCountDTO> collectionCountByRequest(@Param("request") TestPlanCaseModuleRequest request);

    List<TestPlanFunctionalCase> getPlanCaseNotDeletedByCollectionIds(@Param("collectionIds") List<String> collectionIds);

    List<TestPlanResourceExecResultDTO> selectDistinctExecResult(String projectId);

    List<String> selectTestPlanIdByFunctionCaseId(String functionalCaseId);

    List<TestPlanResourceExecResultDTO> selectDistinctExecResultByTestPlanIds(@Param("testPlanIds") List<String> testPlanIds);

    Collection<String> selectIdsByProjectIdsOrCollectionIds(@Param("request") TestPlanCaseMinderRequest request);

    List<FunctionalCaseModule> selectProjectByModuleIds(@Param("moduleIds") List<String> moduleIds);

    Collection<String> selectIdsByModuleIds(@Param("request") TestPlanCaseMinderRequest request, @Param("minderModuleIds") List<String> minderModuleIds);

    Collection<String> selectIdsByRootIds(@Param("rootIds") List<String> rootIds, @Param("testPlanId") String testPlanId);

    List<TestPlanResourceExecResultDTO> selectLastExecResultByTestPlanIds(@Param("testPlanIds") List<String> testPlanIds);

    List<TestPlanResourceExecResultDTO> selectLastExecResultByProjectId(String projectId);
}
