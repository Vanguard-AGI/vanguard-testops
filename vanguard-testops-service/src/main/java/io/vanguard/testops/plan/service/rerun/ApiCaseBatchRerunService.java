package io.vanguard.testops.plan.service.rerun;

import io.vanguard.testops.api.service.definition.ApiTestCaseBatchRunService;
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
public class ApiCaseBatchRerunService implements TaskRerunService {
    @Resource
    private ApiTestCaseBatchRunService apiTestCaseBatchRunService;


    public ApiCaseBatchRerunService() {
        TaskRerunServiceInvoker.register(ExecTaskType.API_CASE_BATCH, this);
    }

    @Override
    public void rerun(ExecTask execTask, String userId) {
        apiTestCaseBatchRunService.rerun(execTask, userId);
    }
}
