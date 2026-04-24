package io.vanguard.testops.api.job;

import io.vanguard.testops.api.domain.ApiScenario;
import io.vanguard.testops.api.mapper.ApiScenarioMapper;
import io.vanguard.testops.api.service.ApiCommonService;
import io.vanguard.testops.api.service.ApiExecuteService;
import io.vanguard.testops.api.service.scenario.ApiScenarioRunService;
import io.vanguard.testops.project.domain.Project;
import io.vanguard.testops.project.mapper.ProjectMapper;
import io.vanguard.testops.sdk.constants.ApiExecuteResourceType;
import io.vanguard.testops.sdk.constants.ApiExecuteRunMode;
import io.vanguard.testops.sdk.constants.ExecTaskType;
import io.vanguard.testops.sdk.constants.TaskTriggerMode;
import io.vanguard.testops.sdk.dto.api.task.ApiRunModeConfigDTO;
import io.vanguard.testops.sdk.dto.api.task.TaskInfo;
import io.vanguard.testops.sdk.dto.api.task.TaskItem;
import io.vanguard.testops.sdk.dto.api.task.TaskRequestDTO;
import io.vanguard.testops.sdk.util.CommonBeanFactory;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.system.domain.ExecTask;
import io.vanguard.testops.system.domain.ExecTaskItem;
import io.vanguard.testops.system.runtime.schedule.BaseScheduleJob;
import io.vanguard.testops.system.service.BaseTaskHubService;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.TriggerKey;

public class ApiScenarioScheduleJob extends BaseScheduleJob {
    @Override
    protected void businessExecute(JobExecutionContext context) {
        ApiExecuteService apiExecuteService = CommonBeanFactory.getBean(ApiExecuteService.class);
        ApiScenarioRunService apiScenarioRunService = CommonBeanFactory.getBean(ApiScenarioRunService.class);
        ApiScenarioMapper apiScenarioMapper = CommonBeanFactory.getBean(ApiScenarioMapper.class);
        ApiCommonService apiCommonService = CommonBeanFactory.getBean(ApiCommonService.class);
        ProjectMapper projectMapper = CommonBeanFactory.getBean(ProjectMapper.class);
        BaseTaskHubService baseTaskHubService = CommonBeanFactory.getBean(BaseTaskHubService.class);
        ApiRunModeConfigDTO apiRunModeConfigDTO = JSON.parseObject(context.getJobDetail().getJobDataMap().get("config").toString(), ApiRunModeConfigDTO.class);

        ApiScenario apiScenario = apiScenarioMapper.selectByPrimaryKey(resourceId);
        if (apiScenario == null) {
            LogUtils.info("当前定时任务的场景已删除 {}", resourceId);
            return;
        }

        Project project = projectMapper.selectByPrimaryKey(apiScenario.getProjectId());
        ExecTask execTask = apiCommonService.newExecTask(project.getId(), userId);
        execTask.setCaseCount(1L);
        execTask.setTaskName(apiScenario.getName());
        execTask.setOrganizationId(project.getOrganizationId());
        execTask.setTriggerMode(TaskTriggerMode.SCHEDULE.name());
        execTask.setTaskType(ExecTaskType.API_SCENARIO.name());

        ExecTaskItem execTaskItem = apiCommonService.newExecTaskItem(execTask.getId(), project.getId(), userId);
        execTaskItem.setOrganizationId(project.getOrganizationId());
        execTaskItem.setResourceType(ApiExecuteResourceType.API_SCENARIO.name());
        execTaskItem.setResourceId(apiScenario.getId());
        execTaskItem.setCaseId(apiScenario.getId());
        execTaskItem.setResourceName(apiScenario.getName());
        baseTaskHubService.insertExecTaskAndDetail(execTask, execTaskItem);

        TaskRequestDTO taskRequest = apiScenarioRunService.getTaskRequest(null, apiScenario.getId(), apiScenario.getProjectId(), ApiExecuteRunMode.SCHEDULE.name());
        TaskInfo taskInfo = taskRequest.getTaskInfo();
        TaskItem taskItem = taskRequest.getTaskItem();
        taskInfo.getRunModeConfig().setPoolId(apiRunModeConfigDTO.getPoolId());
        taskInfo.setTaskId(execTask.getId());
        taskInfo.setSaveResult(true);
        taskInfo.setRealTime(false);
        taskInfo.setUserId(userId);
        taskInfo.getRunModeConfig().setEnvironmentId(apiRunModeConfigDTO.getEnvironmentId());
        taskInfo.getRunModeConfig().setGrouped(apiRunModeConfigDTO.getGrouped());
        taskInfo.setTriggerMode(TaskTriggerMode.SCHEDULE.name());
        taskItem.setId(execTaskItem.getId());

        apiExecuteService.execute(taskRequest);
    }


    public static JobKey getJobKey(String scenarioId) {
        return new JobKey(scenarioId, ApiScenarioScheduleJob.class.getName());
    }

    public static TriggerKey getTriggerKey(String scenarioId) {
        return new TriggerKey(scenarioId, ApiScenarioScheduleJob.class.getName());
    }
}
