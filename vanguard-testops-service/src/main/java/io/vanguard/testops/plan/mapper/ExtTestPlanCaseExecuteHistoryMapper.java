package io.vanguard.testops.plan.mapper;

import io.vanguard.testops.plan.domain.TestPlanCaseExecuteHistory;
import io.vanguard.testops.plan.dto.request.TestPlanCaseExecHistoryRequest;
import io.vanguard.testops.plan.dto.response.TestPlanCaseExecHistoryResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtTestPlanCaseExecuteHistoryMapper {

    void updateDeleted(@Param("testPlanCaseIds") List<String> testPlanCaseIds, @Param("deleted") boolean deleted);

    List<TestPlanCaseExecHistoryResponse> getCaseExecHistory(@Param("request") TestPlanCaseExecHistoryRequest request);

    List<TestPlanCaseExecuteHistory> selectSteps(@Param("testPlanCaseId") String testPlanCaseId, @Param("caseId") String caseId);

    TestPlanCaseExecHistoryResponse getSingleExecHistory(@Param("id") String id);
}
