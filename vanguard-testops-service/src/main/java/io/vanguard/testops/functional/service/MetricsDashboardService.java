package io.vanguard.testops.functional.service;

import io.vanguard.testops.functional.constants.CaseMetricsConstants;
import io.vanguard.testops.functional.domain.CaseMetricsDetail;
import io.vanguard.testops.functional.dto.ProjectOverviewDTO;
import io.vanguard.testops.functional.mapper.CaseMetricsDetailMapper;
import io.vanguard.testops.functional.mapper.ExtCaseMetricsMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

/**
 * 效能数据大屏服务
 * 实现21个核心指标的计算
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MetricsDashboardService {

    @Resource
    private ExtCaseMetricsMapper extCaseMetricsMapper;
    
    @Resource
    private CaseMetricsDetailMapper caseMetricsDetailMapper;

    /**
     * 解析 projectId 为多项目列表，供 Mapper 使用。
     * @param projectId 可能为 null、"ALL"、单 id、或逗号分隔的多个 id（如 "id1,id2"）
     * @return projectIds 为 null 表示“全部项目”；非空列表表示选中的项目 id 列表
     */
    private List<String> parseProjectIds(String projectId) {
        if (!StringUtils.hasText(projectId) || "ALL".equalsIgnoreCase(projectId)) {
            return null;
        }
        String trimmed = projectId.trim();
        if (trimmed.contains(",")) {
            List<String> ids = Arrays.stream(trimmed.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && !"ALL".equalsIgnoreCase(s))
                    .collect(Collectors.toList());
            return ids.isEmpty() ? null : ids;
        }
        return Collections.singletonList(trimmed);
    }

    /**
     * 获取项目概览指标（21个核心指标）
     *
     * @param dimension 维度
     * @param projectId 项目ID
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     */
    public List<ProjectOverviewDTO> getProjectOverview(String dimension, String projectId,
                                                       String userId, Long startTime, Long endTime) {
        ProjectOverviewDTO dto = new ProjectOverviewDTO();
        List<String> projectIds = parseProjectIds(projectId);
        String singleProjectId = (projectIds != null && projectIds.size() == 1) ? projectIds.get(0) : null;
        boolean isAllProjects = (projectIds == null || projectIds.isEmpty());

        try {
            // 确定查询维度
            boolean isPersonalDimension = "personal".equalsIgnoreCase(dimension) ||
                                         (dimension == null && userId != null && !"all".equalsIgnoreCase(userId));
            boolean isAllUsers = userId == null || "all".equalsIgnoreCase(userId);

            // 获取基础用例ID列表
            List<String> baseCaseIds = getBaseCaseIds(singleProjectId, projectIds, userId, isAllProjects, isAllUsers, startTime, endTime);

            // 总用例数 = 按项目查询项目下所有未删除的用例数量（不受时间限制）
            String totalProjectId = (projectIds != null && projectIds.size() == 1) ? projectIds.get(0) : singleProjectId;
            List<String> totalProjectIds = (projectIds != null && projectIds.size() > 1) ? projectIds : null;
            List<String> totalCaseIds = extCaseMetricsMapper.getTotalCaseIdsByProject(totalProjectId, totalProjectIds);
            int totalCaseCount = (totalCaseIds != null) ? totalCaseIds.size() : 0;
            dto.setProjectId(projectId != null && !projectId.isEmpty() ? projectId : "ALL");
            dto.setTotalCaseCount(totalCaseCount);
            // 用例变更热度分母 = 当前项目下总用例数（与总用例数一致，不随时间变动）；提前设好，避免 baseCaseIds 为空时提前 return 导致前端展示 0
            dto.setTotalCaseCountInPeriod((long) totalCaseCount);

            // 有效用例数 = 单位时间内两库中的用例新增数量（按 create_time 在 [startTime, endTime]）
            List<String> twoLibraryNamesForEffective = List.of(
                    CaseMetricsConstants.TwoLibraryModuleName.TEMPLATE_LIBRARY,
                    CaseMetricsConstants.TwoLibraryModuleName.REGRESSION_LIBRARY);
            String effectiveProjectId = (projectIds != null && projectIds.size() == 1) ? projectIds.get(0) : singleProjectId;
            List<String> effectiveProjectIds = (projectIds != null && projectIds.size() > 1) ? projectIds : null;
            List<String> effectiveCaseIds = extCaseMetricsMapper.getTwoLibraryNewCaseIdsInPeriod(
                    effectiveProjectId, effectiveProjectIds, startTime, endTime, twoLibraryNamesForEffective);
            dto.setEffectiveCaseCount(effectiveCaseIds != null ? effectiveCaseIds.size() : 0);

            if (baseCaseIds == null || baseCaseIds.isEmpty()) {
                log.warn("未找到符合条件的用例");
                return Collections.singletonList(dto);
            }

            // 加载所有用例的CS分值数据（作为缓存）
            Map<String, CaseMetricsDetail> allCsMap = loadCsDetailMap(baseCaseIds);

            if (allCsMap.isEmpty()) {
                log.warn("⚠️ 所有用例都没有CS数据！请执行CS初始化");
            }

            // 计算各类指标（无需求筛选，均使用 baseCaseIds）
            List<String> complexityCaseIds = filterCaseIdsByStory(baseCaseIds, null, startTime, endTime);
            Map<String, CaseMetricsDetail> complexityCsMap = filterCsMap(allCsMap, complexityCaseIds);
            calculateComplexityMetrics(dto, complexityCsMap, complexityCaseIds, startTime, endTime);

            List<String> qualityCaseIds = filterCaseIdsByStory(baseCaseIds, null, startTime, endTime);
            calculateQualityMetrics(dto, qualityCaseIds, singleProjectId, projectIds, startTime, endTime);

            List<String> workHourCaseIds = filterCaseIdsByStory(baseCaseIds, null, startTime, endTime);
            Map<String, CaseMetricsDetail> workHourCsMap = filterCsMap(allCsMap, workHourCaseIds);
            calculateTimeDeviationMetrics(dto, workHourCsMap, singleProjectId, projectIds, startTime, endTime, null);
            calculateTimeLevelMetrics(dto, singleProjectId, projectIds, startTime, endTime, null);

            List<String> reuseCaseIds = filterCaseIdsByStory(baseCaseIds, null, startTime, endTime);
            Map<String, CaseMetricsDetail> reuseCsMap = filterCsMap(allCsMap, reuseCaseIds);
            calculateReuseMetrics(dto, reuseCsMap, reuseCaseIds, singleProjectId, projectIds, startTime, endTime);

            calculateChangeMetrics(dto, singleProjectId, projectIds, userId, isAllProjects, isAllUsers,
                    startTime, endTime, null);

            List<String> executionCaseIds = filterCaseIdsByStory(baseCaseIds, null, startTime, endTime);
            calculateExecutionMetrics(dto, executionCaseIds, singleProjectId, projectIds, startTime, endTime);

        } catch (Exception e) {
            log.error("计算项目概览指标失败: projectId={}, userId={}", projectId, userId, e);
            dto.setProjectId(projectId != null && !projectId.isEmpty() ? projectId : "ALL");
        }
        return Collections.singletonList(dto);
    }

    /**
     * 总用例数分母：用例模板库 + 回归用例库 + 最近2周新增（固定为当前时间往前14天，不随请求时间范围变化）
     */
    private int getTotalCaseCountDenominator(String singleProjectId, List<String> projectIds,
                                             boolean isAllProjects) {
        List<String> twoLibraryNames = List.of(
                CaseMetricsConstants.TwoLibraryModuleName.TEMPLATE_LIBRARY,
                CaseMetricsConstants.TwoLibraryModuleName.REGRESSION_LIBRARY);
        String projectId = (projectIds != null && projectIds.size() == 1) ? projectIds.get(0) : singleProjectId;
        List<String> finalProjectIds = (projectIds != null && projectIds.size() > 1) ? projectIds : null;
        String finalProjectId = isAllProjects ? null : (finalProjectIds != null ? null : projectId);

        List<String> twoLibraryCaseIds = extCaseMetricsMapper.getTwoLibraryCaseIds(
                finalProjectId, finalProjectIds, twoLibraryNames);
        long twoLibraryTotal = (twoLibraryCaseIds != null) ? twoLibraryCaseIds.size() : 0L;
        long now = System.currentTimeMillis();
        long start14 = now - 14L * 24 * 3600 * 1000;
        List<String> last14Ids = extCaseMetricsMapper.getNewCaseIdsInTimeRange(finalProjectId, finalProjectIds, start14, now);
        long last14DaysNew = (last14Ids != null) ? last14Ids.size() : 0L;
        return (int) (twoLibraryTotal + last14DaysNew);
    }

    /**
     * 获取基础用例ID列表（不含需求筛选）
     * @param singleProjectId 单个项目 ID（当 projectIds 仅一个时使用）
     * @param projectIds 多项目 ID 列表，null 表示全部项目
     */
    private List<String> getBaseCaseIds(String singleProjectId, List<String> projectIds, String userId,
                                        boolean isAllProjects, boolean isAllUsers,
                                        Long startTime, Long endTime) {
        String finalUserId = (isAllUsers || userId == null || "all".equalsIgnoreCase(userId)) ? null : userId;
        String passProjectId = (projectIds != null && projectIds.size() == 1) ? projectIds.get(0) : singleProjectId;
        if (isAllProjects) {
            passProjectId = null;
            projectIds = null;
        }
        
        List<String> caseIds = extCaseMetricsMapper.getCaseIdsByDimensionAndTime(
                passProjectId, projectIds, finalUserId, startTime, endTime, null, null);
        
        return caseIds != null ? caseIds : Collections.emptyList();
    }

    /**
     * 根据需求ID筛选用例ID列表
     */
    private List<String> filterCaseIdsByStory(List<String> baseCaseIds, List<String> storyIds,
                                              Long startTime, Long endTime) {
        // 如果没有需求筛选，返回原始列表
        if (storyIds == null || storyIds.isEmpty()) {
            return baseCaseIds;
        }
        
        // 根据需求ID获取关联的用例ID
        List<String> caseIdsByRequirements = extCaseMetricsMapper.getCaseIdsByRequirements(
                storyIds, startTime, endTime);
        
        if (caseIdsByRequirements == null || caseIdsByRequirements.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 取交集
        Set<String> requirementCaseSet = new HashSet<>(caseIdsByRequirements);
        return baseCaseIds.stream()
                .filter(requirementCaseSet::contains)
                .collect(Collectors.toList());
    }

    /**
     * 根据用例ID列表过滤CS Map
     */
    private Map<String, CaseMetricsDetail> filterCsMap(Map<String, CaseMetricsDetail> allCsMap, 
                                                       List<String> caseIds) {
        if (caseIds == null || caseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Set<String> caseIdSet = new HashSet<>(caseIds);
        return allCsMap.entrySet().stream()
                .filter(e -> caseIdSet.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * 加载CS分值数据
     */
    private Map<String, CaseMetricsDetail> loadCsDetailMap(List<String> caseIds) {
        if (caseIds == null || caseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        List<CaseMetricsDetail> details = extCaseMetricsMapper.getCsDetailsByCaseIds(caseIds);
        
        if (details == null || details.isEmpty()) {
            return Collections.emptyMap();
        }
        
        return details.stream()
                .filter(d -> d != null && d.getCaseId() != null)
                .collect(Collectors.toMap(
                        CaseMetricsDetail::getCaseId,
                        d -> d,
                        (existing, replacement) -> existing
                ));
    }

    /**
     * 1. 计算复杂度指标（4个）
     */
    private void calculateComplexityMetrics(ProjectOverviewDTO dto, 
                                           Map<String, CaseMetricsDetail> csMap,
                                           List<String> caseIds,
                                           Long startTime, Long endTime) {
        if (csMap.isEmpty()) {
            return;
        }
        
        // 一次遍历完成所有复杂度相关计算
        BigDecimal totalWriteComplexity = BigDecimal.ZERO;
        List<BigDecimal> csScores = new ArrayList<>();
        
        for (CaseMetricsDetail detail : csMap.values()) {
            BigDecimal score = detail.getCsScore();
            if (score != null) {
                totalWriteComplexity = totalWriteComplexity.add(score);
                csScores.add(score);
            }
        }
        
        dto.setTotalWriteComplexity(totalWriteComplexity);
        
        // 执行总复杂分
        List<String> executedCaseIds = extCaseMetricsMapper.getExecutedCaseIdsInTimeRange(
                caseIds, startTime, endTime);
        
        BigDecimal totalExecComplexity = BigDecimal.ZERO;
        if (executedCaseIds != null && !executedCaseIds.isEmpty()) {
            for (String caseId : executedCaseIds) {
                CaseMetricsDetail detail = csMap.get(caseId);
                if (detail != null && detail.getCsScore() != null) {
                    totalExecComplexity = totalExecComplexity.add(detail.getCsScore());
                }
            }
        }
        dto.setTotalExecComplexity(totalExecComplexity);
        
        // 平均复杂度和方差
        if (!csScores.isEmpty()) {
            BigDecimal sum = csScores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avgComplexity = sum.divide(
                    BigDecimal.valueOf(csScores.size()), 2, RoundingMode.HALF_UP);
            dto.setAvgComplexity(avgComplexity);
            
            // 复杂度方差
            BigDecimal variance = csScores.stream()
                    .map(score -> score.subtract(avgComplexity).pow(2))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(csScores.size()), 2, RoundingMode.HALF_UP);
            dto.setComplexityVariance(variance);
        }
    }

    /**
     * 2. 计算UQS质量指标（2个）
     * 复用率：与复用降本一致，范围限定为模板库+回归库，复用定义 case_source_type REUSE/COPY
     */
    private void calculateQualityMetrics(ProjectOverviewDTO dto, List<String> caseIds,
                                        String singleProjectId, List<String> projectIds, Long startTime, Long endTime) {
        String projectId = (projectIds != null && projectIds.size() == 1) ? projectIds.get(0) : singleProjectId;
        if (caseIds == null || caseIds.isEmpty()) {
            dto.setFirstPassRate(BigDecimal.ZERO);
            dto.setAvgUQS(BigDecimal.ZERO);
            dto.setFirstExecCount(0L);
            dto.setFirstPassCount(0L);
            dto.setDefectDiscoveryRate(BigDecimal.ZERO);
            dto.setExecutableRate(BigDecimal.ZERO);
            dto.setReuseExecutionRate(BigDecimal.ZERO);
            return;
        }
        
        Map<String, Object> execStats = extCaseMetricsMapper.getExecutionStats(
                caseIds, startTime, endTime);
        
        if (execStats == null) {
            dto.setFirstPassRate(BigDecimal.ZERO);
            dto.setAvgUQS(BigDecimal.ZERO);
            dto.setFirstExecCount(0L);
            dto.setFirstPassCount(0L);
            dto.setDefectDiscoveryRate(BigDecimal.ZERO);
            dto.setExecutableRate(BigDecimal.ZERO);
            dto.setReuseExecutionRate(BigDecimal.ZERO);
            return;
        }
        
        // 首次通过率：与需求质量视图一致，从 case_execution_record 按 (plan_id, case_id) 取最早一条执行结果统计
        Map<String, Object> firstPassStats = extCaseMetricsMapper.getFirstPassStatsFromCaseExecutionRecord(
                caseIds, startTime, endTime);
        Long firstExecCount = firstPassStats != null ? getLong(firstPassStats.get("first_exec_count")) : null;
        Long firstPassCount = firstPassStats != null ? getLong(firstPassStats.get("first_pass_count")) : null;
        if (firstExecCount == null) {
            firstExecCount = getLong(execStats.get("first_exec_count"));
        }
        if (firstPassCount == null) {
            firstPassCount = getLong(execStats.get("first_pass_count"));
        }
        
        // 设置分子分母
        dto.setFirstExecCount(firstExecCount != null ? firstExecCount : 0L);
        dto.setFirstPassCount(firstPassCount != null ? firstPassCount : 0L);
        
        if (firstExecCount != null && firstExecCount > 0) {
            BigDecimal firstPassRate = BigDecimal.valueOf(firstPassCount != null ? firstPassCount : 0L)
                    .divide(BigDecimal.valueOf(firstExecCount), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            dto.setFirstPassRate(firstPassRate);
        }
        
        // 计算UQS子指标（复用率按两库+REUSE/COPY口径）
        BigDecimal defectDiscoveryRate = calculateDefectDiscoveryRate(caseIds, startTime, endTime);
        BigDecimal executableRate = calculateExecutableRate(execStats);
        BigDecimal reuseExecutionRate = calculateReuseExecutionRate(singleProjectId, projectIds, caseIds, startTime, endTime);
        
        // 设置UQS子指标到DTO（用于前端展示）
        dto.setDefectDiscoveryRate(defectDiscoveryRate);
        dto.setExecutableRate(executableRate);
        dto.setReuseExecutionRate(reuseExecutionRate);
        
        // UQS = 0.4×缺陷发现率 + 0.3×可执行率 + 0.3×复用率
        BigDecimal uqs = defectDiscoveryRate.multiply(new BigDecimal("0.4"))
                .add(executableRate.multiply(new BigDecimal("0.3")))
                .add(reuseExecutionRate.multiply(new BigDecimal("0.3")));
        dto.setAvgUQS(uqs);
    }
    
    private BigDecimal calculateDefectDiscoveryRate(List<String> caseIds, Long startTime, Long endTime) {
        try {
            Long totalExecCases = extCaseMetricsMapper.countExecutedCases(caseIds, startTime, endTime);
            if (totalExecCases == null || totalExecCases == 0) {
                return BigDecimal.ZERO;
            }
            
            Integer totalDefects = extCaseMetricsMapper.getTotalDefectCountForCases(caseIds, startTime, endTime);
            if (totalDefects == null || totalDefects == 0) {
                return BigDecimal.ZERO;
            }
            
            return BigDecimal.valueOf(totalDefects)
                    .divide(BigDecimal.valueOf(totalExecCases), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        } catch (Exception e) {
            log.error("计算缺陷发现率失败", e);
            return BigDecimal.ZERO;
        }
    }
    
    private BigDecimal calculateExecutableRate(Map<String, Object> execStats) {
        try {
            Long totalExecCount = getLong(execStats.get("total_exec_count"));
            Long successWithoutBlockCount = getLong(execStats.get("success_without_block_count"));
            
            if (totalExecCount == null || totalExecCount == 0) {
                return BigDecimal.ZERO;
            }
            
            if (successWithoutBlockCount == null) {
                successWithoutBlockCount = 0L;
            }
            
            return BigDecimal.valueOf(successWithoutBlockCount)
                    .divide(BigDecimal.valueOf(totalExecCount), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        } catch (Exception e) {
            log.error("计算可执行率失败", e);
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * UQS 复用率：与复用降本一致——范围限定为模板库+回归库，复用定义 case_source_type REUSE/COPY
     * 公式：两库内复用用例数 / 两库总用例数 × 100%
     */
    private BigDecimal calculateReuseExecutionRate(String singleProjectId, List<String> projectIds, List<String> caseIds,
                                                   Long startTime, Long endTime) {
        String projectId = (projectIds != null && projectIds.size() == 1) ? projectIds.get(0) : singleProjectId;
        List<String> pids = (projectIds != null && projectIds.size() > 1) ? projectIds : null;
        try {
            List<String> twoLibraryNames = Arrays.asList(
                    CaseMetricsConstants.TwoLibraryModuleName.TEMPLATE_LIBRARY,
                    CaseMetricsConstants.TwoLibraryModuleName.REGRESSION_LIBRARY);
            List<String> twoLibraryCaseIds = extCaseMetricsMapper.getTwoLibraryCaseIds(
                    pids != null ? null : projectId, pids, twoLibraryNames);
            if (twoLibraryCaseIds == null || twoLibraryCaseIds.isEmpty()) {
                return BigDecimal.ZERO;
            }
            List<String> reusedCaseIds = extCaseMetricsMapper.getReusedCaseIdsFromSourceType(
                    pids != null ? null : projectId, pids, null, startTime, endTime);
            if (reusedCaseIds == null || reusedCaseIds.isEmpty()) {
                return BigDecimal.ZERO;
            }
            Set<String> twoLibrarySet = new HashSet<>(twoLibraryCaseIds);
            long reusedInTwoLib = reusedCaseIds.stream().filter(twoLibrarySet::contains).count();
            return BigDecimal.valueOf(reusedInTwoLib)
                    .divide(BigDecimal.valueOf(twoLibraryCaseIds.size()), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        } catch (Exception e) {
            log.error("计算UQS复用率失败", e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 3. 计算工时偏差指标 - 整体（2个）
     * 预期时长：周期内创建且关联需求的测试计划组下的用例预期工时（不去重）
     * 实际编写时长：按测试计划组维度，获取周期内测试计划组关联需求的测分编写时间
     * 实际执行时长：按测试计划组维度，汇总周期内测试计划组下所有测试计划的执行时间
     */
    private void calculateTimeDeviationMetrics(ProjectOverviewDTO dto,
                                              Map<String, CaseMetricsDetail> csMap,
                                              String singleProjectId, List<String> projectIds,
                                              Long startTime, Long endTime,
                                              List<String> storyIds) {
        String projectId = (projectIds != null && projectIds.size() == 1) ? projectIds.get(0) : singleProjectId;
        
        // 设置默认值
        dto.setActualWriteDurationHours(BigDecimal.ZERO);
        dto.setExpectedWriteDurationHours(BigDecimal.ZERO);
        dto.setActualExecDurationMinutes(BigDecimal.ZERO);
        dto.setExpectedExecDurationMinutes(BigDecimal.ZERO);
        
        // 预期编写/执行工时：从周期内创建且关联需求的测试计划组下的用例获取（不去重）
        long totalExpectedWriteMs = 0L;
        long totalExpectedExecMs = 0L;
        try {
            Map<String, Object> expectedTimeStats = extCaseMetricsMapper.getExpectedTimeByTestPlanGroup(
                    projectId, startTime, endTime, storyIds);
            if (expectedTimeStats != null) {
                totalExpectedWriteMs = getLong(expectedTimeStats.get("total_expected_write_ms"));
                totalExpectedExecMs = getLong(expectedTimeStats.get("total_expected_exec_ms"));
            }
        } catch (Exception e) {
            log.error("查询预期工时失败: projectId={}", projectId, e);
        }
        
        // 实际编写工时：按测试计划组维度，获取周期内测试计划组关联需求的测分编写时间
        Map<String, Object> writeTimeStats = null;
        try {
            writeTimeStats = extCaseMetricsMapper.getActualWriteTime(projectId, startTime, endTime, storyIds);
        } catch (Exception e) {
            log.error("查询实际编写时长失败: projectId={}", projectId, e);
        }
        
        if (writeTimeStats != null) {
            Long totalActualWriteMs = getLong(writeTimeStats.get("total_actual_write_ms"));
            
            if (totalActualWriteMs != null && totalActualWriteMs > 0) {
                BigDecimal actualWriteHours = BigDecimal.valueOf(totalActualWriteMs)
                        .divide(BigDecimal.valueOf(1000 * 60 * 60), 4, RoundingMode.HALF_UP);
                dto.setActualWriteDurationHours(actualWriteHours);
                
                if (totalExpectedWriteMs > 0) {
                    BigDecimal expectedWriteHours = BigDecimal.valueOf(totalExpectedWriteMs)
                            .divide(BigDecimal.valueOf(1000 * 60 * 60), 4, RoundingMode.HALF_UP);
                    dto.setExpectedWriteDurationHours(expectedWriteHours);
                    
                    BigDecimal writeDeviation = actualWriteHours.subtract(expectedWriteHours)
                            .abs()
                            .divide(expectedWriteHours, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                    dto.setAvgWriteTimeDeviation(writeDeviation);
                }
            } else if (totalExpectedWriteMs > 0) {
                BigDecimal expectedWriteHours = BigDecimal.valueOf(totalExpectedWriteMs)
                        .divide(BigDecimal.valueOf(1000 * 60 * 60), 4, RoundingMode.HALF_UP);
                dto.setExpectedWriteDurationHours(expectedWriteHours);
            }
        } else if (totalExpectedWriteMs > 0) {
            BigDecimal expectedWriteHours = BigDecimal.valueOf(totalExpectedWriteMs)
                    .divide(BigDecimal.valueOf(1000 * 60 * 60), 4, RoundingMode.HALF_UP);
            dto.setExpectedWriteDurationHours(expectedWriteHours);
        }
        
        // 实际执行工时：按测试计划组维度，汇总周期内测试计划组下所有测试计划的执行时间
        Map<String, Object> execTimeStats = null;
        try {
            execTimeStats = extCaseMetricsMapper.getActualExecutionTime(projectId, startTime, endTime, storyIds);
        } catch (Exception e) {
            log.error("查询实际执行时长失败: projectId={}", projectId, e);
        }
        
        if (execTimeStats != null) {
            Long totalActualExecMs = getLong(execTimeStats.get("total_actual_exec_ms"));
            
            if (totalActualExecMs != null && totalActualExecMs > 0) {
                BigDecimal actualExecMinutes = BigDecimal.valueOf(totalActualExecMs)
                        .divide(BigDecimal.valueOf(1000 * 60), 4, RoundingMode.HALF_UP);
                dto.setActualExecDurationMinutes(actualExecMinutes);
                
                if (totalExpectedExecMs > 0) {
                    BigDecimal expectedExecMinutes = BigDecimal.valueOf(totalExpectedExecMs)
                            .divide(BigDecimal.valueOf(1000 * 60), 4, RoundingMode.HALF_UP);
                    dto.setExpectedExecDurationMinutes(expectedExecMinutes);
                    
                    BigDecimal execDeviation = actualExecMinutes.subtract(expectedExecMinutes)
                            .abs()
                            .divide(expectedExecMinutes, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                    dto.setAvgExecTimeDeviation(execDeviation);
                }
            } else if (totalExpectedExecMs > 0) {
                BigDecimal expectedExecMinutes = BigDecimal.valueOf(totalExpectedExecMs)
                        .divide(BigDecimal.valueOf(1000 * 60), 4, RoundingMode.HALF_UP);
                dto.setExpectedExecDurationMinutes(expectedExecMinutes);
            }
        } else if (totalExpectedExecMs > 0) {
            BigDecimal expectedExecMinutes = BigDecimal.valueOf(totalExpectedExecMs)
                    .divide(BigDecimal.valueOf(1000 * 60), 4, RoundingMode.HALF_UP);
            dto.setExpectedExecDurationMinutes(expectedExecMinutes);
        }
    }

    /**
     * 4. 计算按复杂度分级的工时指标
     * 注意：实际编写时长和实际执行时长不再分级（按测试计划组维度统计），只保留预期时长的分级
     * 预期时长的分级：周期内创建且关联需求的测试计划组下的用例预期工时（不去重，按复杂度分级）
     */
    private void calculateTimeLevelMetrics(ProjectOverviewDTO dto,
                                          String singleProjectId, List<String> projectIds,
                                          Long startTime, Long endTime,
                                          List<String> storyIds) {
        String projectId = (projectIds != null && projectIds.size() == 1) ? projectIds.get(0) : singleProjectId;
        // 按复杂度等级分组统计预期编写时长和预期执行时长
        long l1ExpectedWriteMs = 0, l2ExpectedWriteMs = 0, l3ExpectedWriteMs = 0, l4ExpectedWriteMs = 0;
        long l1ExpectedExecMs = 0, l2ExpectedExecMs = 0, l3ExpectedExecMs = 0, l4ExpectedExecMs = 0;
        
        try {
            List<Map<String, Object>> levelStats = extCaseMetricsMapper.getExpectedTimeByLevelFromTestPlanGroup(
                    projectId, startTime, endTime, storyIds);
            if (levelStats != null) {
                for (Map<String, Object> stat : levelStats) {
                    String level = (String) stat.get("complexity_level");
                    Long expectedWrite = getLong(stat.get("expected_write_ms"));
                    Long expectedExec = getLong(stat.get("expected_exec_ms"));
                    
                    if (level == null) continue;
                    
                    switch (level) {
                        case "L1":
                            if (expectedWrite != null) l1ExpectedWriteMs = expectedWrite;
                            if (expectedExec != null) l1ExpectedExecMs = expectedExec;
                            break;
                        case "L2":
                            if (expectedWrite != null) l2ExpectedWriteMs = expectedWrite;
                            if (expectedExec != null) l2ExpectedExecMs = expectedExec;
                            break;
                        case "L3":
                            if (expectedWrite != null) l3ExpectedWriteMs = expectedWrite;
                            if (expectedExec != null) l3ExpectedExecMs = expectedExec;
                            break;
                        case "L4":
                            if (expectedWrite != null) l4ExpectedWriteMs = expectedWrite;
                            if (expectedExec != null) l4ExpectedExecMs = expectedExec;
                            break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("查询预期工时分级失败: projectId={}", projectId, e);
        }
        
        // 设置预期编写时长（小时）
        setTimeByLevel(dto.getExpectedWriteTime(), l1ExpectedWriteMs, l2ExpectedWriteMs, l3ExpectedWriteMs, l4ExpectedWriteMs, 3600000);
        
        // 设置预期执行时长（分钟）
        setTimeByLevel(dto.getExpectedExecTime(), l1ExpectedExecMs, l2ExpectedExecMs, l3ExpectedExecMs, l4ExpectedExecMs, 60000);
        
        // 注意：实际编写时长和实际执行时长不再分级，保持默认值（0）
        // actualWriteTime 和 actualExecTime 的分级数据不再填充
        // writeTimeDeviationByLevel 和 execTimeDeviationByLevel 也不再计算
    }
    
    private void setTimeByLevel(ProjectOverviewDTO.TimeByLevel target, long l1, long l2, long l3, long l4, long divisor) {
        if (l1 > 0) target.setL1(BigDecimal.valueOf(l1).divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP));
        if (l2 > 0) target.setL2(BigDecimal.valueOf(l2).divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP));
        if (l3 > 0) target.setL3(BigDecimal.valueOf(l3).divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP));
        if (l4 > 0) target.setL4(BigDecimal.valueOf(l4).divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP));
    }

    /**
     * 5. 计算复用降本指标（3个）
     */
    private void calculateReuseMetrics(ProjectOverviewDTO dto, Map<String, CaseMetricsDetail> csMap,
                                      List<String> caseIds, String singleProjectId, List<String> projectIds, Long startTime, Long endTime) {
        String projectId = (projectIds != null && projectIds.size() == 1) ? projectIds.get(0) : singleProjectId;
        List<String> finalProjectIds = (projectIds != null && projectIds.size() > 1) ? projectIds : null;
        if (caseIds == null || caseIds.isEmpty()) {
            dto.setReusedCsTotal(BigDecimal.ZERO);
            dto.setTotalCsScore(BigDecimal.ZERO);
            return;
        }

        List<String> twoLibraryNames = List.of(
                CaseMetricsConstants.TwoLibraryModuleName.TEMPLATE_LIBRARY,
                CaseMetricsConstants.TwoLibraryModuleName.REGRESSION_LIBRARY);

        // 复用用例 = 从两库导入导出的 + 直接复制两库的（仅统计来源用例在用例模板库/回归用例库的 REUSE/COPY）
        List<CaseMetricsDetail> allReusedDetails = extCaseMetricsMapper.getReusedCaseDetailsBySourceType(
                projectId, finalProjectIds, startTime, endTime, twoLibraryNames);

        if (allReusedDetails == null || allReusedDetails.isEmpty()) {
            BigDecimal totalCS = csMap.values().stream()
                    .map(CaseMetricsDetail::getCsScore)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setReusedCsTotal(BigDecimal.ZERO);
            dto.setTotalCsScore(totalCS);
            return;
        }

        Set<String> caseIdSet = new HashSet<>(caseIds);
        List<CaseMetricsDetail> reusedDetails = allReusedDetails.stream()
                .filter(detail -> caseIdSet.contains(detail.getCaseId()))
                .collect(Collectors.toList());

        if (reusedDetails.isEmpty()) {
            BigDecimal totalCS = csMap.values().stream()
                    .map(CaseMetricsDetail::getCsScore)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setReusedCsTotal(BigDecimal.ZERO);
            dto.setTotalCsScore(totalCS);
            return;
        }

        // 直接复用 = 仅改标题；适配复用 = 改了步骤/预期等其它内容；复用用例数 = 直接复用数 + 适配复用数
        long directCount = reusedDetails.stream()
                .filter(d -> CaseMetricsConstants.ReuseType.DIRECT_REUSE.equals(d.getReuseType()))
                .count();
        long adaptCount = reusedDetails.stream()
                .filter(d -> CaseMetricsConstants.ReuseType.ADAPT_REUSE.equals(d.getReuseType()))
                .count();
        int reusedCount = (int) (directCount + adaptCount);

        dto.setReusedCaseCount(reusedCount);
        dto.setDirectReuseCount((int) directCount);
        dto.setAdaptReuseCount((int) adaptCount);

        // 计算用例数量复用率（分母 = 用例模板库 + 回归用例库 + 最近2周新增，最近2周固定为当前时间-14天）
        boolean isAllProjects = (projectIds == null || projectIds.isEmpty());
        int totalDenominator = getTotalCaseCountDenominator(singleProjectId, projectIds, isAllProjects);
        dto.setTotalCaseCountForReuse(totalDenominator);  // 复用指标卡片展示的「总用例数」与分母口径一致
        if (totalDenominator > 0) {
            BigDecimal reuseRateByCount = BigDecimal.valueOf(reusedCount)
                    .divide(BigDecimal.valueOf(totalDenominator), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            dto.setReuseRateByCount(reuseRateByCount);
        } else {
            dto.setReuseRateByCount(BigDecimal.ZERO);
        }
        
        // 计算用例工作量复用率
        BigDecimal reusedCS = reusedDetails.stream()
                .map(CaseMetricsDetail::getCsScore)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCS = csMap.values().stream()
                .map(CaseMetricsDetail::getCsScore)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 设置分子分母
        dto.setReusedCsTotal(reusedCS);
        dto.setTotalCsScore(totalCS);
        
        if (totalCS.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal reuseRateByWorkload = reusedCS
                    .divide(totalCS, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            dto.setReuseRateByWorkload(reuseRateByWorkload);
        }
        
        // 计算绝对节约工时
        BigDecimal savedTimeHours = reusedDetails.stream()
                .map(CaseMetricsDetail::getAlgoExpectedWriteMs)
                .filter(ms -> ms != null && ms > 0)
                .map(ms -> BigDecimal.valueOf(ms).divide(BigDecimal.valueOf(3600000), 2, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        dto.setAbsoluteTimeSavings(savedTimeHours);
    }

    /**
     * 6. 计算变更热度指标（2个）
     */
    private void calculateChangeMetrics(ProjectOverviewDTO dto, String singleProjectId, List<String> projectIds, String userId,
                                       boolean isAllProjects, boolean isAllUsers,
                                       Long startTime, Long endTime, List<String> storyIds) {
        String projectId = (projectIds != null && projectIds.size() == 1) ? projectIds.get(0) : singleProjectId;
        List<String> finalProjectIds = (projectIds != null && projectIds.size() > 1) ? projectIds : null;
        String finalProjectId = isAllProjects ? null : (finalProjectIds != null ? null : projectId);
        String finalUserId = (isAllUsers || userId == null || "all".equalsIgnoreCase(userId)) ? null : userId;

        List<String> twoLibraryNames = List.of(
                CaseMetricsConstants.TwoLibraryModuleName.TEMPLATE_LIBRARY,
                CaseMetricsConstants.TwoLibraryModuleName.REGRESSION_LIBRARY);

        // 用例维度新增率：分子=单位时间内创建的用例汇总数量，分母=两库+最近2周新增（不变）
        List<String> newCaseIdsInPeriod = extCaseMetricsMapper.getNewCaseIdsInTimeRange(
                finalProjectId, finalProjectIds, startTime, endTime);
        if (storyIds != null && !storyIds.isEmpty() && newCaseIdsInPeriod != null && !newCaseIdsInPeriod.isEmpty()) {
            List<String> requirementCaseIds = extCaseMetricsMapper.getCaseIdsByRequirements(
                    storyIds, startTime, endTime);
            if (requirementCaseIds != null && !requirementCaseIds.isEmpty()) {
                Set<String> requirementCaseSet = new HashSet<>(requirementCaseIds);
                newCaseIdsInPeriod = newCaseIdsInPeriod.stream()
                        .filter(requirementCaseSet::contains)
                        .collect(Collectors.toList());
            } else {
                newCaseIdsInPeriod = Collections.emptyList();
            }
        }
        long newCount = (newCaseIdsInPeriod != null) ? newCaseIdsInPeriod.size() : 0L;

        // 两库分母 = 两库模块下当前存在的全部用例数（不受时间影响）
        List<String> twoLibraryCaseIds = extCaseMetricsMapper.getTwoLibraryCaseIds(
                finalProjectId, finalProjectIds, twoLibraryNames);
        long twoLibraryTotal = (twoLibraryCaseIds != null) ? twoLibraryCaseIds.size() : 0L;
        // 用例维度新增率分母 = 两库当前全量 + 最近2周新增（固定为当前时间往前14天，不随请求时间范围变化）
        long now = System.currentTimeMillis();
        long start14 = now - 14L * 24 * 3600 * 1000;
        List<String> last14Ids = extCaseMetricsMapper.getNewCaseIdsInTimeRange(finalProjectId, finalProjectIds, start14, now);
        long last14DaysNew = (last14Ids != null) ? last14Ids.size() : 0L;
        long periodStartCount = twoLibraryTotal + last14DaysNew;
        
        // 用例变更率：分子=变更原因不为 COPY 的用例数量（项目内、不限时间），分母=项目下总用例数（不限时间）
        List<String> modifiedCaseIds = extCaseMetricsMapper.getModifiedCaseIdsFromChangeLog(
                finalProjectId, finalProjectIds, finalUserId, null, null, null);
        long modifiedCount = 0L;
        if (modifiedCaseIds != null && !modifiedCaseIds.isEmpty()) {
            if (storyIds != null && !storyIds.isEmpty()) {
                List<String> requirementCaseIds = extCaseMetricsMapper.getCaseIdsByRequirements(
                        storyIds, startTime, endTime);
                if (requirementCaseIds != null && !requirementCaseIds.isEmpty()) {
                    Set<String> requirementCaseSet = new HashSet<>(requirementCaseIds);
                    modifiedCount = modifiedCaseIds.stream().filter(requirementCaseSet::contains).count();
                }
            } else {
                modifiedCount = modifiedCaseIds.size();
            }
        }
        List<String> totalCaseIds = extCaseMetricsMapper.getTotalCaseIdsByProject(finalProjectId, finalProjectIds);
        long totalCaseCountForChangeRate = (totalCaseIds != null) ? totalCaseIds.size() : 0L;
        BigDecimal caseChangeHeat = BigDecimal.ZERO;
        if (totalCaseCountForChangeRate > 0) {
            caseChangeHeat = BigDecimal.valueOf(modifiedCount)
                    .divide(BigDecimal.valueOf(totalCaseCountForChangeRate), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        dto.setCaseChangeHeat(caseChangeHeat);
        dto.setModifiedCaseCount(modifiedCount);
        dto.setTotalCaseCountInPeriod(totalCaseCountForChangeRate);
        
        // 设置分子分母
        dto.setNewCaseCount(newCount);
        dto.setPeriodStartCaseCount(periodStartCount);
        
        // 计算用例新增率
        if (periodStartCount > 0) {
            BigDecimal growthRate = BigDecimal.valueOf(newCount)
                    .divide(BigDecimal.valueOf(periodStartCount), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            dto.setCaseGrowthRate(growthRate);
        } else if (periodStartCount == 0 && newCount > 0) {
            // 期初为0，但有新增用例，新增率为100%
            dto.setCaseGrowthRate(BigDecimal.valueOf(100));
        } else {
            dto.setCaseGrowthRate(BigDecimal.ZERO);
        }
    }

    /**
     * 7. 计算执行效率指标（3个）
     */
    private void calculateExecutionMetrics(ProjectOverviewDTO dto, List<String> caseIds,
                                          String singleProjectId, List<String> projectIds, Long startTime, Long endTime) {
        String projectId = (projectIds != null && projectIds.size() == 1) ? projectIds.get(0) : singleProjectId;
        if (caseIds == null || caseIds.isEmpty()) {
            dto.setTopFrequentCases(new ArrayList<>());
            dto.setTotalExecDurationMs(0L);
            dto.setTotalExecCount(0L);
            dto.setHighFreqCsTotal(BigDecimal.ZERO);
            dto.setAllExecCsTotal(BigDecimal.ZERO);
            return;
        }
        
        Map<String, Object> execStats = extCaseMetricsMapper.getExecutionStats(
                caseIds, startTime, endTime);
        
        if (execStats != null) {
            // 平均执行时长的分子分母
            Long totalDuration = getLong(execStats.get("total_duration"));
            Long totalExecCount = getLong(execStats.get("total_exec_count"));
            
            // 设置分子分母
            dto.setTotalExecDurationMs(totalDuration != null ? totalDuration : 0L);
            dto.setTotalExecCount(totalExecCount != null ? totalExecCount : 0L);
            
            Long avgDuration = getLong(execStats.get("avg_duration"));
            if (avgDuration != null && avgDuration > 0) {
                dto.setAvgCaseExecDuration(BigDecimal.valueOf(avgDuration)
                        .divide(BigDecimal.valueOf(60000), 2, RoundingMode.HALF_UP));
            }
            
            // 手动用例执行热度：按 source_case_id 归父，仅统计回归用例库（需求口径）
            Map<String, Object> regressionParentStats = extCaseMetricsMapper.getExecutionStatsForRegressionWithParent(
                    projectId, startTime, endTime, CaseMetricsConstants.TwoLibraryModuleName.REGRESSION_LIBRARY);
            Object highFreqCsObj = null;
            Object allExecCsObj = null;
            if (regressionParentStats != null) {
                highFreqCsObj = regressionParentStats.get("high_freq_cs_total");
                allExecCsObj = regressionParentStats.get("all_exec_cs_total");
            }
            if (highFreqCsObj == null || allExecCsObj == null) {
                highFreqCsObj = execStats.get("high_freq_cs_total");
                allExecCsObj = execStats.get("all_exec_cs_total");
            }
            if (highFreqCsObj != null && allExecCsObj != null) {
                BigDecimal highFreqCsTotal = new BigDecimal(highFreqCsObj.toString());
                BigDecimal allExecCsTotal = new BigDecimal(allExecCsObj.toString());
                dto.setHighFreqCsTotal(highFreqCsTotal);
                dto.setAllExecCsTotal(allExecCsTotal);
                if (allExecCsTotal.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal manualHeat = highFreqCsTotal
                            .divide(allExecCsTotal, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                    dto.setManualCaseExecHeat(manualHeat);
                }
            } else {
                dto.setHighFreqCsTotal(BigDecimal.ZERO);
                dto.setAllExecCsTotal(BigDecimal.ZERO);
            }
        } else {
            // execStats 为 null 时设置默认值
            dto.setTotalExecDurationMs(0L);
            dto.setTotalExecCount(0L);
            dto.setHighFreqCsTotal(BigDecimal.ZERO);
            dto.setAllExecCsTotal(BigDecimal.ZERO);
        }
        
        // 高频用例TOP（只统计筛选范围内的用例）
        Set<String> caseIdSet = new HashSet<>(caseIds);
        List<Map<String, Object>> allHighFreqCases = extCaseMetricsMapper.getHighFrequencyCases(
                projectId, 3, startTime, endTime);
        
        if (allHighFreqCases != null && !allHighFreqCases.isEmpty()) {
            // 只保留筛选范围内的用例
            List<ProjectOverviewDTO.TopFrequentCase> topCases = allHighFreqCases.stream()
                    .filter(item -> {
                        String caseId = (String) item.get("case_id");
                        return caseId != null && caseIdSet.contains(caseId);
                    })
                    .map(this::convertToTopFrequentCase)
                    .collect(Collectors.toList());
            dto.setTopFrequentCases(topCases);
        } else {
            dto.setTopFrequentCases(new ArrayList<>());
        }
    }
    
    private ProjectOverviewDTO.TopFrequentCase convertToTopFrequentCase(Map<String, Object> item) {
        ProjectOverviewDTO.TopFrequentCase topCase = new ProjectOverviewDTO.TopFrequentCase();
        topCase.setCaseId((String) item.get("case_id"));
        topCase.setCaseName((String) item.get("case_name"));
        
        Long execCountLong = getLong(item.get("exec_count"));
        topCase.setExecCount(execCountLong != null ? execCountLong.intValue() : 0);
        
        Object csScore = item.get("cs_score");
        if (csScore != null) {
            try {
                if (csScore instanceof BigDecimal) {
                    topCase.setComplexity((BigDecimal) csScore);
                } else {
                    topCase.setComplexity(new BigDecimal(csScore.toString()));
                }
            } catch (Exception e) {
                topCase.setComplexity(BigDecimal.ZERO);
            }
        } else {
            topCase.setComplexity(BigDecimal.ZERO);
        }
        return topCase;
    }

    // ========== 辅助方法 ==========

    private void calculateDeviation(ProjectOverviewDTO.TimeByLevel deviation,
                                   ProjectOverviewDTO.TimeByLevel expected,
                                   ProjectOverviewDTO.TimeByLevel actual) {
        if (expected.getL1().compareTo(BigDecimal.ZERO) > 0) {
            deviation.setL1(actual.getL1().subtract(expected.getL1())
                    .abs()
                    .divide(expected.getL1(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)));
        }
        if (expected.getL2().compareTo(BigDecimal.ZERO) > 0) {
            deviation.setL2(actual.getL2().subtract(expected.getL2())
                    .abs()
                    .divide(expected.getL2(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)));
        }
        if (expected.getL3().compareTo(BigDecimal.ZERO) > 0) {
            deviation.setL3(actual.getL3().subtract(expected.getL3())
                    .abs()
                    .divide(expected.getL3(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)));
        }
        if (expected.getL4().compareTo(BigDecimal.ZERO) > 0) {
            deviation.setL4(actual.getL4().subtract(expected.getL4())
                    .abs()
                    .divide(expected.getL4(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)));
        }
    }

    private Long getLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof BigDecimal) return ((BigDecimal) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 获取用例变更原因分布统计
     */
    public Map<String, Long> getChangeReasonDistribution(String projectId, String userId,
                                                          Long startTime, Long endTime) {
        try {
            List<Map<String, Object>> distribution = extCaseMetricsMapper.getChangeReasonDistribution(
                    projectId, userId, startTime, endTime, null);
            
            Map<String, Long> result = new LinkedHashMap<>();
            if (distribution != null && !distribution.isEmpty()) {
                for (Map<String, Object> item : distribution) {
                    String reason = (String) item.get("change_reason");
                    Long count = getLong(item.get("count"));
                    if (reason != null && count != null) {
                        result.put(reason, count);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            log.error("获取变更原因分布失败", e);
            return new HashMap<>();
        }
    }

    /**
     * 获取测试用例执行阻塞原因分布统计
     */
    public Map<String, Long> getBlockedReasonDistribution(String projectId, String userId,
                                                           Long startTime, Long endTime) {
        try {
            List<Map<String, Object>> distribution = extCaseMetricsMapper.getBlockedReasonDistribution(
                    projectId, userId, startTime, endTime, null);
            
            Map<String, Long> result = new LinkedHashMap<>();
            if (distribution != null && !distribution.isEmpty()) {
                for (Map<String, Object> item : distribution) {
                    String reason = (String) item.get("block_reason");
                    Long count = getLong(item.get("count"));
                    if (reason != null && count != null) {
                        result.put(reason, count);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            log.error("获取阻塞原因分布失败", e);
            return new HashMap<>();
        }
    }

    /**
     * 获取需求列表（支持模糊搜索）
     */
    public List<io.vanguard.testops.functional.dto.dashboard.RequirementDTO> getRequirementsList(
            String keyword, String projectId, Long startTime, Long endTime) {
        try {
            return extCaseMetricsMapper.getRequirementsList(keyword, projectId, startTime, endTime);
        } catch (Exception e) {
            log.error("获取需求列表失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 根据变更原因查询用例列表（含需求信息）
     */
    public List<io.vanguard.testops.functional.dto.dashboard.CaseWithRequirementDTO> getCasesByChangeReason(
            String changeReason, String projectId, String userId, Long startTime, Long endTime) {
        try {
            return extCaseMetricsMapper.getCasesByChangeReason(
                    changeReason, projectId, userId, startTime, endTime);
        } catch (Exception e) {
            log.error("根据变更原因查询用例失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 根据阻塞原因查询用例列表（含需求信息）
     */
    public List<io.vanguard.testops.functional.dto.dashboard.CaseWithRequirementDTO> getCasesByBlockReason(
            String blockReason, String projectId, String userId, Long startTime, Long endTime) {
        try {
            return extCaseMetricsMapper.getCasesByBlockReason(
                    blockReason, projectId, userId, startTime, endTime);
        } catch (Exception e) {
            log.error("根据阻塞原因查询用例失败", e);
            return new ArrayList<>();
        }
    }
}
