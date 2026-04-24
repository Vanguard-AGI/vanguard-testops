package io.vanguard.testops.provider;

import java.util.List;

public interface BaseTestPlanProvider {
    List<String> selectTestPlanIdByFunctionCaseAndStatus(String caseId, List<String> statusList);
}
