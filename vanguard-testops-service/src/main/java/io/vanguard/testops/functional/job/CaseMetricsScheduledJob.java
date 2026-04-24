package io.vanguard.testops.functional.job;

import io.vanguard.testops.functional.service.CaseCSCalculationService;
import io.vanguard.testops.functional.service.CaseMetricsService;
import io.vanguard.testops.functional.service.FeishuMeegoService;
import io.vanguard.testops.sdk.util.LogUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * 用例效能指标统一定时任务
 * 包括：CS值计算、每日效能指标计算、飞书需求同步
 */
@Component
public class CaseMetricsScheduledJob {

    @Resource
    private CaseCSCalculationService caseCSCalculationService;

    @Resource
    private CaseMetricsService caseMetricsService;

    @Resource
    private FeishuMeegoService feishuMeegoService;

    @Value("${feishu.meego.default-user-email:}")
    private String defaultUserEmail;

    /**
     * 用例CS值批量计算
     * 执行时间：每天凌晨2点（默认，可通过配置修改）
     * 说明：异步计算所有用例的CS复杂度分值
     */
    @Scheduled(cron = "${metrics.schedule.case-cs:0 0 2 * * ?}")
    public void calculateCaseCS() {
        LogUtils.info("========== 定时任务：用例CS值计算 ==========");
        try {
            caseCSCalculationService.batchCalculateMetricsDetailAsync(null, true);
            LogUtils.info("用例CS值计算任务已提交至异步线程池");
        } catch (Exception e) {
            LogUtils.error("提交用例CS值计算任务失败", e);
        }
    }

    /**
     * 每日效能指标计算
     * 执行时间：每天凌晨4点
     * 说明：计算昨天的项目级和用户级效能指标
     * 已禁用：不需要此定时任务
     */
    // @Scheduled(cron = "${metrics.schedule.case-metrics-daily:0 0 4 * * ?}")
    public void calculateDailyMetrics() {
        LogUtils.info("========== 定时任务：每日效能指标计算 ==========");
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            ZonedDateTime startOfDay = yesterday.atStartOfDay(ZoneId.systemDefault());
            ZonedDateTime endOfDay = yesterday.plusDays(1).atStartOfDay(ZoneId.systemDefault());
            
            long startTime = startOfDay.toInstant().toEpochMilli();
            long endTime = endOfDay.toInstant().toEpochMilli();
            
            // 计算项目维度指标
            caseMetricsService.calculateAndSaveMetrics(null, "PROJECT", "DAY", 
                    yesterday, startTime, endTime);
            
            // 计算用户维度指标
            caseMetricsService.calculateAndSaveMetrics(null, "USER", "DAY", 
                    yesterday, startTime, endTime);
            
            LogUtils.info("每日效能指标计算完成，快照日期: {}", yesterday);
        } catch (Exception e) {
            LogUtils.error("每日效能指标计算失败", e);
        }
    }

    // /**
    //  * 每周效能指标计算
    //  * 执行时间：每周一凌晨4点
    //  * 说明：计算上周的项目级和用户级效能指标
    //  */
    // @Scheduled(cron = "${metrics.schedule.case-metrics-weekly:0 0 4 ? * MON}")
    // public void calculateWeeklyMetrics() {
    //     LogUtils.info("========== 定时任务：每周效能指标计算 ==========");
    //     try {
    //         LocalDate today = LocalDate.now();
    //         LocalDate lastMonday = today.minusWeeks(1).with(java.time.DayOfWeek.MONDAY);
    //         LocalDate lastSunday = lastMonday.plusDays(6);
    //         
    //         ZonedDateTime startOfWeek = lastMonday.atStartOfDay(ZoneId.systemDefault());
    //         ZonedDateTime endOfWeek = lastSunday.plusDays(1).atStartOfDay(ZoneId.systemDefault());
    //         
    //         long startTime = startOfWeek.toInstant().toEpochMilli();
    //         long endTime = endOfWeek.toInstant().toEpochMilli();
    //         
    //         LocalDate snapshotDate = lastSunday;
    //         
    //         // 计算项目维度指标
    //         caseMetricsService.calculateAndSaveMetrics(null, "PROJECT", "WEEK", 
    //                 snapshotDate, startTime, endTime);
    //         
    //         // 计算用户维度指标
    //         caseMetricsService.calculateAndSaveMetrics(null, "USER", "WEEK", 
    //                 snapshotDate, startTime, endTime);
    //         
    //         LogUtils.info("每周效能指标计算完成，快照日期: {}, 时间范围: {} 至 {}", 
    //                 snapshotDate, lastMonday, lastSunday);
    //     } catch (Exception e) {
    //         LogUtils.error("每周效能指标计算失败", e);
    //     }
    // }

    // /**
    //  * 每月效能指标计算
    //  * 执行时间：每月1号凌晨4点
    //  * 说明：计算上个月的项目级和用户级效能指标
    //  */
    // @Scheduled(cron = "${metrics.schedule.case-metrics-monthly:0 0 4 1 * ?}")
    // public void calculateMonthlyMetrics() {
    //     LogUtils.info("========== 定时任务：每月效能指标计算 ==========");
    //     try {
    //         LocalDate today = LocalDate.now();
    //         LocalDate firstDayOfLastMonth = today.minusMonths(1).withDayOfMonth(1);
    //         LocalDate lastDayOfLastMonth = today.withDayOfMonth(1).minusDays(1);
    //         
    //         ZonedDateTime startOfMonth = firstDayOfLastMonth.atStartOfDay(ZoneId.systemDefault());
    //         ZonedDateTime endOfMonth = lastDayOfLastMonth.plusDays(1).atStartOfDay(ZoneId.systemDefault());
    //         
    //         long startTime = startOfMonth.toInstant().toEpochMilli();
    //         long endTime = endOfMonth.toInstant().toEpochMilli();
    //         
    //         LocalDate snapshotDate = lastDayOfLastMonth;
    //         
    //         // 计算项目维度指标
    //         caseMetricsService.calculateAndSaveMetrics(null, "PROJECT", "MONTH", 
    //                 snapshotDate, startTime, endTime);
    //         
    //         // 计算用户维度指标
    //         caseMetricsService.calculateAndSaveMetrics(null, "USER", "MONTH", 
    //                 snapshotDate, startTime, endTime);
    //         
    //         LogUtils.info("每月效能指标计算完成，快照日期: {}, 时间范围: {} 至 {}", 
    //                 snapshotDate, firstDayOfLastMonth, lastDayOfLastMonth);
    //     } catch (Exception e) {
    //         LogUtils.error("每月效能指标计算失败", e);
    //     }
    // }

    /**
     * 飞书需求定时同步
     * 执行时间：每天凌晨1点（默认，可通过配置修改）
     * 说明：异步同步飞书Meego需求数据
     */
    @Scheduled(cron = "${metrics.schedule.feishu-sync:0 0 1 * * ?}")
    public void syncFeishuStories() {
        if (StringUtils.isBlank(defaultUserEmail)) {
            LogUtils.debug("飞书需求同步：未配置 feishu.meego.default-user-email，跳过同步");
            return;
        }
        
        LogUtils.info("========== 定时任务：飞书需求同步 ==========");
        try {
            feishuMeegoService.syncStoriesAsync(defaultUserEmail);
            LogUtils.info("飞书需求同步任务已提交至异步线程池");
        } catch (Exception e) {
            LogUtils.error("提交飞书需求同步任务失败", e);
        }
    }
}

