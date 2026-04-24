package io.vanguard.testops.plan.service.rerun;

import io.vanguard.testops.plan.service.TestPlanExecuteService;
import io.vanguard.testops.sdk.constants.ExecTaskType;
import io.vanguard.testops.system.domain.ExecTask;
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
public class TestPlanRerunService implements TaskRerunService {
    @Resource
    private TestPlanExecuteService testPlanExecuteService;

    public TestPlanRerunService() {
        TaskRerunServiceInvoker.register(ExecTaskType.TEST_PLAN, this);
    }

    @Override
    public void rerun(ExecTask execTask,  String userId) {
        testPlanExecuteService.testPlanOrGroupRerun(execTask, userId);
    }
}
