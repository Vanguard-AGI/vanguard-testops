package io.vanguard.testops.plan.job;

import io.vanguard.testops.plan.dto.request.TestPlanExecuteRequest;
import io.vanguard.testops.plan.service.TestPlanExecuteService;
import io.vanguard.testops.sdk.constants.ApiBatchRunMode;
import io.vanguard.testops.sdk.constants.ApiExecuteRunMode;
import io.vanguard.testops.sdk.util.CommonBeanFactory;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.system.runtime.schedule.BaseScheduleJob;
import io.vanguard.testops.system.uid.IDGenerator;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.TriggerKey;

import java.util.Map;

public class TestPlanScheduleJob extends BaseScheduleJob {

    @Override
    protected void businessExecute(JobExecutionContext context) {
        TestPlanExecuteService testPlanExecuteService = CommonBeanFactory.getBean(TestPlanExecuteService.class);
        assert testPlanExecuteService != null;
        Map<String, String> runConfig = JSON.parseObject(context.getJobDetail().getJobDataMap().get("config").toString(), Map.class);
        String runMode = runConfig.containsKey("runMode") ? runConfig.get("runMode") : ApiBatchRunMode.SERIAL.name();
        LogUtils.info("开始执行测试计划的定时任务. ID：" + resourceId);
        Thread.startVirtualThread(() ->
            testPlanExecuteService.singleExecuteTestPlan(new TestPlanExecuteRequest() {{
                this.setExecuteId(resourceId);
                this.setRunMode(runMode);
                this.setExecutionSource(ApiExecuteRunMode.SCHEDULE.name());
            }}, IDGenerator.nextStr(), userId)
        );
    }


    public static JobKey getJobKey(String testPlanId) {
        return new JobKey(testPlanId, TestPlanScheduleJob.class.getName());
    }

    public static TriggerKey getTriggerKey(String testPlanId) {
        return new TriggerKey(testPlanId, TestPlanScheduleJob.class.getName());
    }
}
