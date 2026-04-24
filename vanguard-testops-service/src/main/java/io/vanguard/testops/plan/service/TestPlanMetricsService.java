package io.vanguard.testops.plan.service;

import io.vanguard.testops.functional.domain.CaseMetricsDetail;
import io.vanguard.testops.functional.domain.MeegoStoryStats;
import io.vanguard.testops.functional.mapper.CaseMetricsDetailMapper;
import io.vanguard.testops.functional.mapper.MeegoStoryStatsMapper;
import io.vanguard.testops.functional.service.CaseCSCalculationService;
import io.vanguard.testops.plan.domain.TestPlan;
import io.vanguard.testops.plan.domain.TestPlanExample;
import io.vanguard.testops.plan.domain.TestPlanMetrics;
import io.vanguard.testops.plan.mapper.ExtTestPlanFunctionalCaseMapper;
import io.vanguard.testops.plan.mapper.TestPlanMapper;
import io.vanguard.testops.plan.mapper.TestPlanMetricsMapper;
import io.vanguard.testops.plan.mapper.TestPlanCaseMetricsMapper;
import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.system.uid.IDGenerator;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 测试计划指标计算服务
 * 实现测试计划级别的指标聚合和偏差率计算
 */
@Slf4j
@Service
public class TestPlanMetricsService {

    @Resource
    private TestPlanMapper testPlanMapper;

    @Resource
    private TestPlanMetricsMapper testPlanMetricsMapper;

    @Resource
    private ExtTestPlanFunctionalCaseMapper extTestPlanFunctionalCaseMapper;

    @Resource
    private CaseMetricsDetailMapper caseMetricsDetailMapper;

    @Resource
    private MeegoStoryStatsMapper meegoStoryStatsMapper;

    @Resource
    private TestPlanCaseMetricsMapper testPlanCaseMetricsMapper;

    @Resource
    private CaseCSCalculationService caseCSCalculationService;

    /**
     * 计算并保存测试计划的所有指标
     * 每个测试计划独立事务，避免长事务
     * 
     * @param testPlanId 测试计划ID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void calculateAndSaveMetrics(String testPlanId) {
        if (StringUtils.isBlank(testPlanId)) {
            log.warn("测试计划ID为空，跳过计算");
            return;
        }

        try {
            TestPlan testPlan = testPlanMapper.selectByPrimaryKey(testPlanId);
            if (testPlan == null) {
                log.warn("测试计划不存在: {}", testPlanId);
                return;
            }

            TestPlanMetrics metrics = testPlanMetricsMapper.selectByTestPlanId(testPlanId);
            boolean isNewRecord = false;
            if (metrics == null) {
                metrics = new TestPlanMetrics();
                metrics.setId(IDGenerator.nextStr());
                metrics.setTestPlanId(testPlanId);
                metrics.setProjectId(testPlan.getProjectId());
                metrics.setEnvInstabilityFactor(BigDecimal.valueOf(1.0));
                isNewRecord = true;
            }

            long now = System.currentTimeMillis();
            metrics.setLastCalcTime(now);

            List<String> caseIds = getTestPlanCaseIds(testPlanId);
            
            if (CollectionUtils.isEmpty(caseIds)) {
                log.warn("测试计划 {} 没有关联用例，跳过计算", testPlanId);
                return;
            }

            metrics.setTotalCases(caseIds.size());

            aggregateExpectedMetrics(metrics, caseIds);
            aggregateActualExecMetrics(metrics, testPlanId);
            calculateWriteDeviationRate(metrics, testPlan);
            calculateExecDeviationRate(metrics);
            calculateFirstPassRate(metrics, testPlanId);
            calculateDefectCount(metrics, testPlan);

            if (isNewRecord) {
                testPlanMetricsMapper.insert(metrics);
                log.info("创建测试计划指标记录: testPlanId={}", testPlanId);
            } else {
                int updateResult = testPlanMetricsMapper.updateByPrimaryKey(metrics);
                if (updateResult > 0) {
                    log.info("更新测试计划指标记录: testPlanId={}", testPlanId);
                } else {
                    log.warn("更新测试计划指标记录失败(0行被更新)，尝试插入新记录: testPlanId={}, metricsId={}", testPlanId, metrics.getId());
                    try {
                        testPlanMetricsMapper.insert(metrics);
                        log.info("插入新记录成功: testPlanId={}", testPlanId);
                    } catch (Exception insertEx) {
                        log.error("插入新记录也失败: testPlanId={}", testPlanId, insertEx);
                    }
                }
            }

        } catch (Exception e) {
            log.error("计算测试计划指标失败: testPlanId={}", testPlanId, e);
            throw e;
        }
    }

    /**
     * 获取测试计划关联的所有用例ID
     */
    private List<String> getTestPlanCaseIds(String testPlanId) {
        List<io.vanguard.testops.plan.domain.TestPlanFunctionalCase> planCases = 
            extTestPlanFunctionalCaseMapper.selectByTestPlanIdAndNotDeleted(testPlanId);
        
        if (CollectionUtils.isEmpty(planCases)) {
            return List.of();
        }
        
        return planCases.stream()
            .map(io.vanguard.testops.plan.domain.TestPlanFunctionalCase::getFunctionalCaseId)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());
    }

    /**
     * 聚合预期编写时长和预期执行时长（优化：批量查询）
     */
    private void aggregateExpectedMetrics(TestPlanMetrics metrics, List<String> caseIds) {
        // 批量查询所有用例的指标详情（性能优化：从 N 次查询优化为 1 次查询）
        List<CaseMetricsDetail> detailList = caseMetricsDetailMapper.selectByCaseIds(caseIds);
        
        // 检查是否有用例的预期时间为0或不存在，需要重新计算
        List<String> needRecalculateCaseIds = new ArrayList<>();
        List<String> existingCaseIds = new ArrayList<>();
        
        for (CaseMetricsDetail detail : detailList) {
            if (detail != null) {
                existingCaseIds.add(detail.getCaseId());
                Long expectedWriteMs = detail.getAlgoExpectedWriteMs();
                Long expectedExecMs = detail.getAlgoExpectedExecMs();
                if (expectedWriteMs == null || expectedWriteMs <= 0 || 
                    expectedExecMs == null || expectedExecMs <= 0) {
                    needRecalculateCaseIds.add(detail.getCaseId());
                }
            }
        }
        
        // 检查是否有用例在 case_metrics_detail 表中没有记录
        for (String caseId : caseIds) {
            if (!existingCaseIds.contains(caseId)) {
                needRecalculateCaseIds.add(caseId);
            }
        }
        
        // 去重
        needRecalculateCaseIds = needRecalculateCaseIds.stream()
            .distinct()
            .collect(Collectors.toList());
        
        // 如果有需要重新计算的用例，触发重新计算
        if (!needRecalculateCaseIds.isEmpty()) {
            log.warn("发现 {} 个用例的预期编写/执行时间为0或不存在，触发重新计算CS指标: caseIds={}", 
                needRecalculateCaseIds.size(), needRecalculateCaseIds);
            
            for (String caseId : needRecalculateCaseIds) {
                try {
                    caseCSCalculationService.recalculateCaseCS(caseId);
                    log.debug("重新计算用例CS指标完成: caseId={}", caseId);
                } catch (Exception e) {
                    log.error("重新计算用例CS指标失败: caseId={}", caseId, e);
                }
            }
            
            // 重新查询这些用例的指标详情
            detailList = caseMetricsDetailMapper.selectByCaseIds(caseIds);
        }
        
        long totalAlgoWriteMs = 0L;
        long totalAlgoExecMs = 0L;
        BigDecimal totalCsScore = BigDecimal.ZERO;
        BigDecimal totalCsScoreSq = BigDecimal.ZERO;
        int validCaseCount = 0;

        for (CaseMetricsDetail detail : detailList) {
            if (detail != null) {
                if (detail.getAlgoExpectedWriteMs() != null) {
                    totalAlgoWriteMs += detail.getAlgoExpectedWriteMs();
                }
                if (detail.getAlgoExpectedExecMs() != null) {
                    totalAlgoExecMs += detail.getAlgoExpectedExecMs();
                }
                if (detail.getCsScore() != null) {
                    totalCsScore = totalCsScore.add(detail.getCsScore());
                    totalCsScoreSq = totalCsScoreSq.add(detail.getCsScore().pow(2));
                    validCaseCount++;
                }
            }
        }

        metrics.setTotalAlgoWriteMs(totalAlgoWriteMs);
        metrics.setTotalAlgoExecMs(totalAlgoExecMs);

        if (validCaseCount > 0) {
            BigDecimal count = BigDecimal.valueOf(validCaseCount);
            BigDecimal avgCsScore = totalCsScore.divide(count, 2, RoundingMode.HALF_UP);
            metrics.setAvgCsScore(avgCsScore);
            
            BigDecimal avgSq = totalCsScoreSq.divide(count, 4, RoundingMode.HALF_UP);
            BigDecimal variance = avgSq.subtract(avgCsScore.pow(2)).setScale(2, RoundingMode.HALF_UP);
            if (variance.compareTo(BigDecimal.ZERO) < 0) {
                variance = BigDecimal.ZERO;
            }
            metrics.setComplexityVariance(variance);
        } else {
            metrics.setAvgCsScore(BigDecimal.ZERO);
            metrics.setComplexityVariance(BigDecimal.ZERO);
        }
    }

    /**
     * 聚合实际执行时长（已批量查询优化）
     */
    private void aggregateActualExecMetrics(TestPlanMetrics metrics, String testPlanId) {
        List<io.vanguard.testops.plan.domain.TestPlanCaseMetrics> planCaseMetrics = 
            testPlanCaseMetricsMapper.selectByTestPlanId(testPlanId);
        
        if (CollectionUtils.isEmpty(planCaseMetrics)) {
            metrics.setTotalActualExecMs(0L);
            metrics.setTotalActualReadingMs(0L);
            metrics.setTotalActualTimeMs(0L);
            metrics.setReuseSavingHours(BigDecimal.ZERO);
            return;
        }

        long totalActualExecMs = 0L;
        long totalActualReadingMs = 0L;
        List<String> caseIds = new ArrayList<>();

        for (io.vanguard.testops.plan.domain.TestPlanCaseMetrics planCaseMetric : planCaseMetrics) {
            caseIds.add(planCaseMetric.getCaseId());
            
            if (planCaseMetric.getActualExecMs() != null) {
                totalActualExecMs += planCaseMetric.getActualExecMs();
            }
            if (planCaseMetric.getActualReadingMs() != null) {
                totalActualReadingMs += planCaseMetric.getActualReadingMs();
            }
        }

        metrics.setTotalActualExecMs(totalActualExecMs);
        metrics.setTotalActualReadingMs(totalActualReadingMs);
        
        // 自动计算测试计划总耗时
        long totalActualTimeMs = totalActualExecMs + totalActualReadingMs;
        metrics.setTotalActualTimeMs(totalActualTimeMs);
        
        // 从 case_metrics_detail 查询这些用例的节约工时和工作量复用率
        long totalSavedWriteMs = 0L;
        long reuseWorkloadMs = 0L;  // 复用用例的理论编写工时总和
        long totalWorkloadMs = 0L;   // 所有用例的理论编写工时总和
        
        if (!caseIds.isEmpty()) {
            List<io.vanguard.testops.functional.domain.CaseMetricsDetail> csDetails = 
                caseMetricsDetailMapper.selectByCaseIds(caseIds);
            
            for (io.vanguard.testops.functional.domain.CaseMetricsDetail detail : csDetails) {
                // 计算节约工时
                if (detail.getSavedWriteMs() != null) {
                    totalSavedWriteMs += detail.getSavedWriteMs();
                }
                
                // 计算用例工作量复用率
                Long algoExpectedWriteMs = detail.getAlgoExpectedWriteMs();
                if (algoExpectedWriteMs != null && algoExpectedWriteMs > 0) {
                    totalWorkloadMs += algoExpectedWriteMs;
                    
                    // 如果是复用用例，累计到复用工作量
                    String sourceType = detail.getCaseSourceType();
                    if ("REUSE".equals(sourceType) || "COPY".equals(sourceType)) {
                        reuseWorkloadMs += algoExpectedWriteMs;
                    }
                }
            }
        }
        
        // 设置复用总节约工时（小时）
        BigDecimal hours = BigDecimal.valueOf(totalSavedWriteMs)
            .divide(BigDecimal.valueOf(1000 * 60 * 60), 2, RoundingMode.HALF_UP);
        metrics.setReuseSavingHours(hours);
        
        // 计算用例工作量复用率 = (复用用例总工作量 / 计划总工作量) × 100%
        if (totalWorkloadMs > 0) {
            BigDecimal reuseWorkloadRate = BigDecimal.valueOf(reuseWorkloadMs)
                .divide(BigDecimal.valueOf(totalWorkloadMs), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
            
            log.info("测试计划 {} 用例工作量复用率: {}%, 复用工作量={}ms, 总工作量={}ms", 
                testPlanId, reuseWorkloadRate, reuseWorkloadMs, totalWorkloadMs);
            
            // 注意：如果 TestPlanMetrics 实体中有 reuseWorkloadRate 字段，可以设置
            // metrics.setReuseWorkloadRate(reuseWorkloadRate);
        }
    }

    /**
     * 计算编写时长偏差率
     * 实际编写时长从飞书需求获取，预期编写时长从用例指标聚合
     */
    private void calculateWriteDeviationRate(TestPlanMetrics metrics, TestPlan testPlan) {
        if (StringUtils.isBlank(testPlan.getFeishuStoryId())) {
            log.debug("测试计划 {} 未关联飞书需求，编写时长偏差率设为0", testPlan.getId());
            metrics.setWriteDeviationRate(BigDecimal.ZERO);
            return;
        }

        MeegoStoryStats storyStats = meegoStoryStatsMapper.selectByStoryId(testPlan.getFeishuStoryId());
        if (storyStats == null || storyStats.getTestAnalysisTime() == null 
            || storyStats.getTestAnalysisTime() <= 0) {
            log.debug("飞书需求 {} 的测分时间为空，编写时长偏差率设为0", testPlan.getFeishuStoryId());
            metrics.setWriteDeviationRate(BigDecimal.ZERO);
            return;
        }

        long actualWriteMs = convertDaysToMs(BigDecimal.valueOf(storyStats.getTestAnalysisTime()));
        long expectedWriteMs = metrics.getTotalAlgoWriteMs() != null ? metrics.getTotalAlgoWriteMs() : 0L;

        if (expectedWriteMs <= 0) {
            log.warn("测试计划 {} 的预期编写时长为0或为空，这不应该发生。用例创建/更新时应该已计算预期编写时间。可能原因：1) 用例在CS计算功能实现前创建；2) CS计算失败。建议：重新计算这些用例的CS指标。actualWriteMs={}, expectedWriteMs={}, testPlanId={}", 
                testPlan.getId(), actualWriteMs, expectedWriteMs, testPlan.getId());
            metrics.setWriteDeviationRate(BigDecimal.ZERO);
            return;
        }

        // 计算偏差率前先检查是否会超出范围，避免除零或异常大的值
        BigDecimal actualWriteMsBd = BigDecimal.valueOf(actualWriteMs);
        BigDecimal expectedWriteMsBd = BigDecimal.valueOf(expectedWriteMs);
        BigDecimal diff = actualWriteMsBd.subtract(expectedWriteMsBd).abs();
        
        // 如果差值超过预期值的 999.99 倍，直接设置为最大值，避免计算溢出
        BigDecimal maxDiff = expectedWriteMsBd.multiply(new BigDecimal("999.99"));
        if (diff.compareTo(maxDiff) > 0) {
            log.warn("编写时长偏差率将超出范围，直接设置为999.99%: testPlanId={}, actualWriteMs={}, expectedWriteMs={}, diff={}",
                testPlan.getId(), actualWriteMs, expectedWriteMs, diff);
            metrics.setWriteDeviationRate(new BigDecimal("999.99"));
            return;
        }

        BigDecimal deviationRate = diff
            .divide(expectedWriteMsBd, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_UP);

        // 限制偏差率在 DECIMAL(5,2) 范围内：-999.99 到 999.99
        BigDecimal maxDeviationRate = new BigDecimal("999.99");
        BigDecimal minDeviationRate = new BigDecimal("-999.99");
        if (deviationRate.compareTo(maxDeviationRate) > 0) {
            deviationRate = maxDeviationRate;
            log.warn("编写时长偏差率超出范围，已限制为999.99%: testPlanId={}, actualWriteMs={}, expectedWriteMs={}, calculatedRate={}%",
                testPlan.getId(), actualWriteMs, expectedWriteMs, deviationRate);
        } else if (deviationRate.compareTo(minDeviationRate) < 0) {
            deviationRate = minDeviationRate;
            log.warn("编写时长偏差率超出范围，已限制为-999.99%: testPlanId={}, actualWriteMs={}, expectedWriteMs={}, calculatedRate={}%",
                testPlan.getId(), actualWriteMs, expectedWriteMs, deviationRate);
        }

        metrics.setWriteDeviationRate(deviationRate);
        log.debug("计算编写时长偏差率: testPlanId={}, actualWriteMs={}, expectedWriteMs={}, deviationRate={}%",
            testPlan.getId(), actualWriteMs, expectedWriteMs, deviationRate);
    }

    /**
     * 计算执行时长偏差率
     */
    private void calculateExecDeviationRate(TestPlanMetrics metrics) {
        long actualExecMs = metrics.getTotalActualExecMs() != null ? metrics.getTotalActualExecMs() : 0L;
        long expectedExecMs = metrics.getTotalAlgoExecMs() != null ? metrics.getTotalAlgoExecMs() : 0L;

        if (expectedExecMs <= 0) {
            metrics.setAvgExecDeviationRate(BigDecimal.ZERO);
            return;
        }

        BigDecimal deviationRate = BigDecimal.valueOf(Math.abs(actualExecMs - expectedExecMs))
            .divide(BigDecimal.valueOf(expectedExecMs), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_UP);

        // 限制偏差率在 DECIMAL(5,2) 范围内：-999.99 到 999.99
        BigDecimal maxDeviationRate = new BigDecimal("999.99");
        BigDecimal minDeviationRate = new BigDecimal("-999.99");
        if (deviationRate.compareTo(maxDeviationRate) > 0) {
            deviationRate = maxDeviationRate;
            log.warn("执行时长偏差率超出范围，已限制为999.99%: actualExecMs={}, expectedExecMs={}",
                actualExecMs, expectedExecMs);
        } else if (deviationRate.compareTo(minDeviationRate) < 0) {
            deviationRate = minDeviationRate;
        }

        metrics.setAvgExecDeviationRate(deviationRate);
    }

    /**
     * 计算首次通过率
     */
    private void calculateFirstPassRate(TestPlanMetrics metrics, String testPlanId) {
        List<io.vanguard.testops.plan.domain.TestPlanCaseMetrics> planCaseMetrics = 
            testPlanCaseMetricsMapper.selectByTestPlanId(testPlanId);
            
        if (CollectionUtils.isEmpty(planCaseMetrics)) {
            metrics.setFirstPassRate(BigDecimal.ZERO);
            return;
        }
        
        long firstExecCount = 0;
        long firstPassCount = 0;
        
        for (io.vanguard.testops.plan.domain.TestPlanCaseMetrics metric : planCaseMetrics) {
            if (metric.getFirstExecTime() != null && metric.getFirstExecTime() > 0) {
                firstExecCount++;
                if ("PASS".equals(metric.getFirstExecResult())) {
                    firstPassCount++;
                }
            }
        }
        
        if (firstExecCount > 0) {
            BigDecimal rate = BigDecimal.valueOf(firstPassCount)
                .divide(BigDecimal.valueOf(firstExecCount), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
            metrics.setFirstPassRate(rate);
        } else {
            metrics.setFirstPassRate(BigDecimal.ZERO);
        }
    }

    /**
     * 将人天转换为毫秒
     * 1人天 = 8小时 = 8 * 60 * 60 * 1000 毫秒
     */
    private long convertDaysToMs(BigDecimal days) {
        if (days == null || days.compareTo(BigDecimal.ZERO) <= 0) {
            return 0L;
        }
        return days.multiply(BigDecimal.valueOf(8 * 60 * 60 * 1000))
            .longValue();
    }

    /**
     * 批量计算指定项目下所有测试计划的指标（异步执行）
     */
    @Async("meticsCalculationExecutor")
    public void calculateByProjectIdAsync(String projectId) {
        if (StringUtils.isBlank(projectId)) {
            return;
        }

        log.info("========== 异步计算项目 {} 的测试计划指标 ==========", projectId);
        
        TestPlanExample example = new TestPlanExample();
        example.createCriteria()
            .andProjectIdEqualTo(projectId)
            .andTypeEqualTo("TEST_PLAN")
            .andStatusEqualTo("NOT_ARCHIVED");

        List<TestPlan> testPlans = testPlanMapper.selectByExample(example);
        
        if (CollectionUtils.isEmpty(testPlans)) {
            log.warn("项目 {} 下没有找到测试计划", projectId);
            return;
        }

        int successCount = 0;
        int failCount = 0;
        
        for (TestPlan testPlan : testPlans) {
            try {
                calculateAndSaveMetrics(testPlan.getId());
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("计算测试计划指标失败: testPlanId={}", testPlan.getId(), e);
            }
        }
        
        log.info("========== 项目 {} 计算完成：成功 {}, 失败 {} ==========", projectId, successCount, failCount);
    }
    
    /**
     * 批量计算指定项目下所有测试计划的指标（同步方法，返回计数）
     */
    public int calculateByProjectId(String projectId) {
        if (StringUtils.isBlank(projectId)) {
            return 0;
        }

        TestPlanExample example = new TestPlanExample();
        example.createCriteria()
            .andProjectIdEqualTo(projectId)
            .andTypeEqualTo("TEST_PLAN")
            .andStatusEqualTo("NOT_ARCHIVED");

        List<TestPlan> testPlans = testPlanMapper.selectByExample(example);
        int totalCount = testPlans == null ? 0 : testPlans.size();
        
        // 触发异步计算
        calculateByProjectIdAsync(projectId);
        
        log.info("项目 {} 的计算任务已提交至异步线程池，共 {} 个测试计划", projectId, totalCount);
        
        return totalCount;
    }

    /**
     * 计算单个测试计划的指标（异步执行，不阻塞主线程）
     * 用于测试计划新增/更新时自动触发
     */
    @Async("meticsCalculationExecutor")
    public void calculateAndSaveMetricsAsync(String testPlanId) {
        if (StringUtils.isBlank(testPlanId)) {
            return;
        }
        try {
            calculateAndSaveMetrics(testPlanId);
            log.debug("测试计划指标计算完成: testPlanId={}", testPlanId);
        } catch (Exception e) {
            log.error("测试计划指标计算失败: testPlanId={}", testPlanId, e);
        }
    }
    
    /**
     * 批量计算所有测试计划的指标（异步执行，不阻塞主线程）
     */
    @Async("meticsCalculationExecutor")
    public void calculateAllAsync() {
        log.info("========== 异步批量计算任务开始 ==========");
        
        TestPlanExample example = new TestPlanExample();
        example.createCriteria()
            .andTypeEqualTo("TEST_PLAN")
            .andStatusEqualTo("NOT_ARCHIVED");

        List<TestPlan> testPlans = testPlanMapper.selectByExample(example);
        
        if (CollectionUtils.isEmpty(testPlans)) {
            log.warn("未找到需要计算指标的测试计划");
            return;
        }

        log.info("开始批量计算测试计划指标，共 {} 个测试计划", testPlans.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (TestPlan testPlan : testPlans) {
            try {
                // 每个测试计划独立事务
                calculateAndSaveMetrics(testPlan.getId());
                successCount++;
                
                if (successCount % 50 == 0) {
                    log.info("批量计算进度: 已完成 {}/{}", successCount, testPlans.size());
                }
            } catch (Exception e) {
                failCount++;
                log.error("计算测试计划指标失败: testPlanId={}", testPlan.getId(), e);
            }
        }

        log.info("========== 异步批量计算任务完成：成功 {}, 失败 {}, 总数 {} ==========", successCount, failCount, testPlans.size());
    }
    
    /**
     * 批量计算所有测试计划的指标（同步方法，用于返回计数）
     * 实际计算通过异步方法执行
     */
    public int calculateAll() {
        TestPlanExample example = new TestPlanExample();
        example.createCriteria()
            .andTypeEqualTo("TEST_PLAN")
            .andStatusEqualTo("NOT_ARCHIVED");

        List<TestPlan> testPlans = testPlanMapper.selectByExample(example);
        int totalCount = testPlans == null ? 0 : testPlans.size();
        
        // 触发异步计算
        calculateAllAsync();
        
        log.info("批量计划任务已提交至异步线程池，共 {} 个测试计划", totalCount);
        
        return totalCount;
    }

    /**
     * 计算测试计划的缺陷数量（从飞书需求读取）
     * 
     * @param metrics TestPlanMetrics
     * @param testPlan TestPlan
     */
    private void calculateDefectCount(TestPlanMetrics metrics, TestPlan testPlan) {
        try {
            // 测试计划如果关联了飞书需求，从 meego_story_stats 读取缺陷数
            if (StringUtils.isNotBlank(testPlan.getFeishuStoryId())) {
                MeegoStoryStats storyStats = meegoStoryStatsMapper.selectByStoryId(testPlan.getFeishuStoryId());
                
                if (storyStats != null && storyStats.getDefectCount() != null) {
                    metrics.setTotalDefectCount(storyStats.getDefectCount());
                    log.info("从飞书需求读取缺陷数: testPlanId={}, storyId={}, defectCount={}", 
                        testPlan.getId(), testPlan.getFeishuStoryId(), storyStats.getDefectCount());
                } else {
                    metrics.setTotalDefectCount(0);
                    log.warn("飞书需求无缺陷数据: testPlanId={}, storyId={}", 
                        testPlan.getId(), testPlan.getFeishuStoryId());
                }
            } else {
                // 未关联飞书需求，缺陷数为0
                metrics.setTotalDefectCount(0);
            }
        } catch (Exception e) {
            log.error("查询飞书需求缺陷数失败: testPlanId=" + testPlan.getId(), e);
            metrics.setTotalDefectCount(0);
        }
    }
}

