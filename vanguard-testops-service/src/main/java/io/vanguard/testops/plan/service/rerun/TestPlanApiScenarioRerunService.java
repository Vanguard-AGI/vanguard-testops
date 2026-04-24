package io.vanguard.testops.plan.service.rerun;

import io.vanguard.testops.api.service.ApiCommonService;
import io.vanguard.testops.plan.service.TestPlanApiScenarioService;
import io.vanguard.testops.sdk.constants.ExecTaskType;
import io.vanguard.testops.system.domain.ExecTask;
import io.vanguard.testops.system.domain.ExecTaskItem;
import io.vanguard.testops.system.invoker.TaskRerunServiceInvoker;
import io.vanguard.testops.system.service.TaskRerunService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author: Jan
 * @CreateTime: 2024-02-06  20:47
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class TestPlanApiScenarioRerunService implements TaskRerunService {
    @Resource
    private TestPlanApiScenarioService testPlanApiScenarioService;
    @Resource
    private ApiCommonService apiCommonService;


    public TestPlanApiScenarioRerunService() {
        TaskRerunServiceInvoker.register(ExecTaskType.TEST_PLAN_API_SCENARIO, this);
    }

    @Override
    public void rerun(ExecTask execTask, String userId) {
        ExecTaskItem execTaskItem = apiCommonService.getRerunTaskItem(execTask.getId());
        testPlanApiScenarioService.runRun(execTask, execTaskItem, userId);
    }
}
