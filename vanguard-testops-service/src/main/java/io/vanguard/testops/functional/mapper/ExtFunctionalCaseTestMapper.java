package io.vanguard.testops.functional.mapper;

import io.vanguard.testops.api.domain.ApiScenario;
import io.vanguard.testops.api.domain.ApiTestCase;
import io.vanguard.testops.functional.domain.FunctionalCaseTest;
import io.vanguard.testops.functional.dto.FunctionalCaseTestDTO;
import io.vanguard.testops.functional.dto.FunctionalCaseTestPlanDTO;
import io.vanguard.testops.functional.dto.TestPlanCaseExecuteHistoryDTO;
import io.vanguard.testops.functional.request.AssociatePlanPageRequest;
import io.vanguard.testops.functional.request.DisassociateOtherCaseRequest;
import io.vanguard.testops.functional.request.FunctionalCaseTestRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtFunctionalCaseTestMapper {

    List<String> getIds(@Param("request") DisassociateOtherCaseRequest request);

    Integer getOtherCaseCount(@Param("caseId") String caseId);

    List<FunctionalCaseTestDTO> getList(@Param("request") FunctionalCaseTestRequest request);

    List<FunctionalCaseTestPlanDTO> getPlanList(@Param("request") AssociatePlanPageRequest request);

    List<TestPlanCaseExecuteHistoryDTO> getPlanExecuteHistoryList(@Param("caseId") String caseId, @Param("planId") String planId);

    List<ApiTestCase> selectApiCaseByCaseIds(@Param("isRepeat") boolean isRepeat, @Param("caseIds") List<String> caseIds, @Param("testPlanId") String testPlanId);

    List<ApiScenario> selectApiScenarioByCaseIds(@Param("isRepeat") boolean isRepeat, @Param("caseIds") List<String> caseIds, @Param("testPlanId") String testPlanId);

    List<FunctionalCaseTest> selectApiAndScenarioIdsFromCaseIds(@Param("caseIds") List<String> functionalCaseIds);
}
