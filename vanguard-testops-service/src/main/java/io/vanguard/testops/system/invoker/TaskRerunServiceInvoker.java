package io.vanguard.testops.system.invoker;

import io.vanguard.testops.sdk.constants.ExecTaskType;
import io.vanguard.testops.sdk.util.EnumValidator;
import io.vanguard.testops.system.domain.ExecTask;
import io.vanguard.testops.system.service.TaskRerunService;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: Jan
 * @CreateTime: 2024-02-06  20:48
 */
public class TaskRerunServiceInvoker {

    private static final Map<ExecTaskType, TaskRerunService> taskRerunServiceMap = new HashMap<>();

    public static void register(ExecTaskType execTaskType, TaskRerunService taskRerunService) {
        taskRerunServiceMap.put(execTaskType, taskRerunService);
    }

    private static TaskRerunService getTaskRerunService(ExecTaskType execTaskType) {
        return taskRerunServiceMap.get(execTaskType);
    }

    public static ExecTaskType getExecTaskType(String execTaskType) {
        return EnumValidator.validateEnum(ExecTaskType.class, execTaskType);
    }

    public static void rerun(ExecTask execTask, String userId) {
        TaskRerunService taskRerunService = getTaskRerunService(getExecTaskType(execTask.getTaskType()));
        if (taskRerunService != null) {
            taskRerunService.rerun(execTask, userId);
        }
    }
}
