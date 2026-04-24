package io.vanguard.testops.plan.service.rerun;

import io.vanguard.testops.api.service.scenario.ApiScenarioBatchRunService;
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
public class ApiScenarioBatchRerunService implements TaskRerunService {
    @Resource
    private ApiScenarioBatchRunService apiScenarioBatchRunService;


    public ApiScenarioBatchRerunService() {
        TaskRerunServiceInvoker.register(ExecTaskType.API_SCENARIO_BATCH, this);
    }

    @Override
    public void rerun(ExecTask execTask, String userId) {
        apiScenarioBatchRunService.rerun(execTask, userId);
    }
}
