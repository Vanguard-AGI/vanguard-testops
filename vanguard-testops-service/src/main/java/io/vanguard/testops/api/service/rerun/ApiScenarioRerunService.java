package io.vanguard.testops.api.service.rerun;

import io.vanguard.testops.api.service.ApiCommonService;
import io.vanguard.testops.api.service.scenario.ApiScenarioRunService;
import io.vanguard.testops.sdk.constants.ExecTaskType;
import io.vanguard.testops.system.domain.ExecTask;
import io.vanguard.testops.system.domain.ExecTaskItem;
import io.vanguard.testops.system.invoker.TaskRerunServiceInvoker;
import io.vanguard.testops.system.service.TaskRerunService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @Author: Jan
 * @CreateTime: 2024-02-06  20:47
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ApiScenarioRerunService implements TaskRerunService {
    @Resource
    private ApiScenarioRunService apiScenarioRunService;
    @Resource
    private ApiCommonService apiCommonService;

    public ApiScenarioRerunService() {
        TaskRerunServiceInvoker.register(ExecTaskType.API_SCENARIO, this);
    }

    @Override
    public void rerun(ExecTask execTask, String userId) {
        ExecTaskItem execTaskItem = apiCommonService.getRerunTaskItem(execTask.getId());
        apiScenarioRunService.runRun(execTask, execTaskItem, userId);
    }
}
