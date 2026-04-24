package io.vanguard.testops.plan;

import io.vanguard.testops.plan.service.TestPlanManagementService;
import io.vanguard.testops.provider.BaseTestPlanProvider;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("TEST-PLAN")
public class TestPlanProvider implements BaseTestPlanProvider {

    @Resource
    private TestPlanManagementService testPlanManagementService;

    @Override
    public List<String> selectTestPlanIdByFunctionCaseAndStatus(String caseId, List<String> statusList) {
        return testPlanManagementService.selectTestPlanIdByFuncCaseIdAndStatus(caseId, statusList);
    }
}
