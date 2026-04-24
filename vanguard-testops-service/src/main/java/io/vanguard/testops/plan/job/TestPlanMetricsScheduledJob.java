package io.vanguard.testops.plan.job;

import io.vanguard.testops.plan.service.TestPlanMetricsService;
import io.vanguard.testops.sdk.util.LogUtils;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 测试计划指标计算定时任务
 * 每天凌晨3点执行，异步计算所有未归档测试计划的指标数据
 * 配置项：metrics.schedule.test-plan（默认 0 0 3 * * ?）
 */
@Component
public class TestPlanMetricsScheduledJob {

    @Resource
    private TestPlanMetricsService testPlanMetricsService;

    /**
     * 每天凌晨3点执行测试计划指标计算（异步执行）
     */
    @Scheduled(cron = "${metrics.schedule.test-plan:0 0 3 * * ?}")
    public void calculateTestPlanMetrics() {
        LogUtils.info("========== 定时任务：测试计划指标计算 ==========");
        try {
            testPlanMetricsService.calculateAllAsync();
            LogUtils.info("测试计划指标计算任务已提交至异步线程池");
        } catch (Exception e) {
            LogUtils.error("提交测试计划指标计算任务失败", e);
        }
    }
}

