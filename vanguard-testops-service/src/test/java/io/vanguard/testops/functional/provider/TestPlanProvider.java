package io.vanguard.testops.functional.provider;

import io.vanguard.testops.provider.BaseTestPlanProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("TEST-PLAN")
public class TestPlanProvider implements BaseTestPlanProvider {

    @Override
    public List<String> selectTestPlanIdByFunctionCaseAndStatus(String caseId, List<String> statusList) {
        return new ArrayList<>();
    }
}
