package io.vanguard.testops.functional.service;

import io.vanguard.testops.functional.constants.CaseMetricsConstants;
import io.vanguard.testops.functional.domain.CaseMetricsDetail;
import io.vanguard.testops.functional.domain.CaseExecutionRecordAggregate;
import io.vanguard.testops.functional.domain.CaseMetricsData;
import io.vanguard.testops.functional.domain.FunctionalCase;
import io.vanguard.testops.functional.domain.FunctionalCaseBlob;
import io.vanguard.testops.functional.domain.FunctionalCaseExample;
import io.vanguard.testops.functional.dto.CaseDetailWithCSDTO;
import io.vanguard.testops.functional.dto.CaseListResponseDTO;
import io.vanguard.testops.functional.dto.CaseMetricsDTO;
import io.vanguard.testops.functional.dto.PlanGroupMetricsDTO;
import io.vanguard.testops.functional.dto.CSMetricsDTO;
import io.vanguard.testops.functional.mapper.*;
import io.vanguard.testops.functional.request.CaseListQueryRequest;
import io.vanguard.testops.functional.request.CaseMetricsQueryRequest;
import io.vanguard.testops.functional.request.CSMetricsQueryRequest;
import io.vanguard.testops.sdk.util.BeanUtils;
import io.vanguard.testops.system.uid.IDGenerator;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用例效能指标服务
 * 实现11个核心指标的计算和查询
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CaseMetricsService {

    @Resource
    private CaseCSCalculationService csCalculationService;
    
    @Resource
    private CaseMetricsDataMapper caseMetricsDataMapper;
    
    @Resource
    private CaseMetricsDetailMapper caseMetricsDetailMapper;
    
    @Resource
    private CaseExecutionRecordMapper caseExecutionRecordMapper;
    
    @Resource
    private ExtCaseMetricsMapper extCaseMetricsMapper;
    
    @Resource
    private FunctionalCaseMapper functionalCaseMapper;
    
    @Resource
    private FunctionalCaseBlobMapper functionalCaseBlobMapper;
    
    @Resource
    private FunctionalCaseModuleMapper functionalCaseModuleMapper;
    
    @Resource
    private io.vanguard.testops.system.mapper.UserMapper userMapper;

    /**
     * 获取综合指标（11个核心指标）
     */
    public CaseMetricsDTO getComprehensiveMetrics(CaseMetricsQueryRequest request) {
        log.info("查询综合指标: projectId={}, startTime={}, endTime={}", 
                request.getProjectId(), request.getStartTime(), request.getEndTime());
        
        CaseMetricsDTO dto = new CaseMetricsDTO();
        dto.setProjectId(request.getProjectId());
        dto.setTimeRangeStart(request.getStartTime());
        dto.setTimeRangeEnd(request.getEndTime());
        
        String projectId = request.getProjectId();
        Long startTime = request.getStartTime();
        Long endTime = request.getEndTime();
        Map<String, CaseMetricsDetail> csCacheMap = loadProjectCaseMetricsDetail(projectId);
        CaseExecutionRecordAggregate projectExecutionSummary = loadExecutionSummaryByProject(projectId, startTime, endTime);
        
        try {
            dto.setAvgCaseCS(calculateAvgCaseCS(csCacheMap));
            
            dto.setCaseOutputRate(calculateCaseOutputRate(projectId, startTime, endTime, csCacheMap));
            
            dto.setCaseChangeHeat(calculateCaseChangeHeat(projectId, startTime, endTime));
            
            dto.setHighValueExecRate(calculateHighValueExecRate(projectId, startTime, endTime, csCacheMap));
            
            dto.setTimeSavingRate(calculateTimeSavingRate(projectId, startTime, endTime));
            
            dto.setPlanCaseReuseRate(calculatePlanCaseReuseRate(projectId, startTime, endTime));
            
            dto.setPlanCaseModifyRate(calculatePlanCaseModifyRate(projectId, startTime, endTime));
            
            dto.setPlanCaseNewRate(calculatePlanCaseNewRate(projectId, startTime, endTime));
            
            dto.setPlanAvgExecDuration(calculatePlanAvgExecDuration(projectExecutionSummary));
            
            dto.setPlanPassRate(calculatePlanPassRate(projectExecutionSummary));
            
            dto.setPlanFirstPassRate(calculatePlanFirstPassRate(projectExecutionSummary));
            
            Long totalCount = extCaseMetricsMapper.countCasesByProject(projectId);
            dto.setTotalCaseCount(totalCount != null ? totalCount.intValue() : 0);
            
            Long completedCount = extCaseMetricsMapper.countCasesByProjectAndTimeRange(projectId, startTime, endTime);
            dto.setCompletedCaseCount(completedCount != null ? completedCount.intValue() : 0);
            
            BigDecimal totalCS = calculateTotalCS(csCacheMap);
            dto.setTotalCS(totalCS);
            
            List<Map<String, Object>> highFreqCases = extCaseMetricsMapper.getHighFrequencyCases(
                    projectId, CaseMetricsConstants.HIGH_FREQ_THRESHOLD, startTime, endTime);
            dto.setHighFreqCaseCount(highFreqCases != null ? highFreqCases.size() : 0);
            
        } catch (Exception e) {
            log.error("计算综合指标失败: projectId={}", projectId, e);
        }
        
        return dto;
    }

    /**
     * 计算并保存指标数据
     * 该方法由定时任务调用
     * @param projectId 项目ID，如果为null则计算所有项目
     * @param metricLevel 指标级别：PROJECT-项目, USER-用户
     * @param timeDimension 时间维度：DAY-日, WEEK-周, MONTH-月
     * @param snapshotDate 快照日期（通常是昨天）
     * @param startTime 统计时间范围-开始（毫秒时间戳）
     * @param endTime 统计时间范围-结束（毫秒时间戳）
     */
    public void calculateAndSaveMetrics(String projectId, String metricLevel, String timeDimension, 
                                       java.time.LocalDate snapshotDate, Long startTime, Long endTime) {
        log.info("开始计算指标数据: projectId={}, metricLevel={}, timeDimension={}, snapshotDate={}", 
                projectId, metricLevel, timeDimension, snapshotDate);
        
        try {
            CaseMetricsDTO metrics;
            
            if ("USER".equals(metricLevel)) {
                // 用户维度：需要为每个用户计算
                List<String> userIds = extCaseMetricsMapper.getAllUserIds();
                if (userIds == null || userIds.isEmpty()) {
                    log.warn("未找到需要计算指标的用户");
                    return;
                }
                
                for (String userId : userIds) {
                    metrics = getComprehensiveMetricsByUser(userId, startTime, endTime);
                    saveMetricsData(projectId, userId, metricLevel, timeDimension, snapshotDate, 
                                  startTime, endTime, metrics);
                }
            } else {
                // 项目维度
                if (projectId != null) {
                    // 计算指定项目
                    CaseMetricsQueryRequest request = new CaseMetricsQueryRequest();
                    request.setProjectId(projectId);
                    request.setStartTime(startTime);
                    request.setEndTime(endTime);
                    metrics = getComprehensiveMetrics(request);
                    saveMetricsData(projectId, null, metricLevel, timeDimension, snapshotDate, 
                                  startTime, endTime, metrics);
                } else {
                    // 计算所有项目
                    List<String> projectIds = extCaseMetricsMapper.getAllProjectIds();
                    if (projectIds == null || projectIds.isEmpty()) {
                        log.warn("未找到需要计算指标的项目");
                        return;
                    }
                    
                    for (String pid : projectIds) {
                        CaseMetricsQueryRequest request = new CaseMetricsQueryRequest();
                        request.setProjectId(pid);
                        request.setStartTime(startTime);
                        request.setEndTime(endTime);
                        metrics = getComprehensiveMetrics(request);
                        saveMetricsData(pid, null, metricLevel, timeDimension, snapshotDate, 
                                      startTime, endTime, metrics);
                    }
                }
            }
            
            log.info("指标数据计算完成: projectId={}, metricLevel={}, timeDimension={}", 
                    projectId, metricLevel, timeDimension);
        } catch (Exception e) {
            log.error("计算指标数据失败: projectId={}, metricLevel={}, timeDimension={}", 
                     projectId, metricLevel, timeDimension, e);
            throw e;
        }
    }
    
    /**
     * 保存指标数据到 case_metrics_detail 表
     */
    private void saveMetricsData(String projectId, String userId, String metricLevel, 
                                String timeDimension, java.time.LocalDate snapshotDate,
                                Long startTime, Long endTime, CaseMetricsDTO metrics) {
        try {
            // 检查是否已存在相同条件的记录（根据唯一约束：project_id, user_id, metric_level, time_dimension, snapshot_date）
            CaseMetricsData existing = caseMetricsDataMapper.selectLatestByProject(
                    projectId, metricLevel, timeDimension);
            
            // 构建指标数据对象
            CaseMetricsData metricsData = new CaseMetricsData();
            metricsData.setProjectId(projectId);
            metricsData.setUserId(userId);
            metricsData.setMetricLevel(metricLevel);
            metricsData.setTimeDimension(timeDimension);
            metricsData.setSnapshotDate(snapshotDate);
            metricsData.setTimeRangeStart(startTime);
            metricsData.setTimeRangeEnd(endTime);
            
            // 填充指标数据
            metricsData.setTotalCsScore(metrics.getTotalCS());
            metricsData.setAvgCsScore(metrics.getAvgCaseCS());
            metricsData.setCompletedCaseCount(metrics.getCompletedCaseCount());
            metricsData.setCaseOutputRate(metrics.getCaseOutputRate());
            metricsData.setModifiedCaseCount(metrics.getCaseChangeHeat());
            metricsData.setChangeHeat(metrics.getCaseChangeHeat());
            metricsData.setHighFreqCaseCount(metrics.getHighFreqCaseCount());
            metricsData.setHighValueExecRate(metrics.getHighValueExecRate());
            metricsData.setTimeSavingRate(metrics.getTimeSavingRate());
            metricsData.setPlanCaseReuseRate(metrics.getPlanCaseReuseRate());
            metricsData.setPlanCaseModifyRate(metrics.getPlanCaseModifyRate());
            metricsData.setPlanCaseNewRate(metrics.getPlanCaseNewRate());
            // planAvgExecDuration 在 CaseMetricsDTO 中是 Long（分钟），需要转换为毫秒
            if (metrics.getPlanAvgExecDuration() != null) {
                metricsData.setPlanAvgExecDuration(metrics.getPlanAvgExecDuration() * 60 * 1000);
            }
            metricsData.setPlanPassRate(metrics.getPlanPassRate());
            metricsData.setPlanFirstPassRate(metrics.getPlanFirstPassRate());
            metricsData.setTotalCaseCount(metrics.getTotalCaseCount());
            metricsData.setIsCalculated(1);
            metricsData.setCalculationTime(System.currentTimeMillis());
            metricsData.setUpdateTime(System.currentTimeMillis());
            
            if (existing != null && existing.getSnapshotDate() != null && 
                existing.getSnapshotDate().equals(snapshotDate) &&
                ((userId == null && existing.getUserId() == null) || 
                 (userId != null && userId.equals(existing.getUserId())))) {
                // 更新现有记录
                metricsData.setId(existing.getId());
                metricsData.setCreateTime(existing.getCreateTime());
                caseMetricsDataMapper.updateByPrimaryKeySelective(metricsData);
                log.debug("更新指标数据: projectId={}, userId={}, metricLevel={}, timeDimension={}, snapshotDate={}", 
                         projectId, userId, metricLevel, timeDimension, snapshotDate);
            } else {
                // 插入新记录
                metricsData.setId(IDGenerator.nextStr());
                metricsData.setCreateTime(System.currentTimeMillis());
                caseMetricsDataMapper.insertSelective(metricsData);
                log.debug("插入指标数据: projectId={}, userId={}, metricLevel={}, timeDimension={}, snapshotDate={}", 
                         projectId, userId, metricLevel, timeDimension, snapshotDate);
            }
        } catch (Exception e) {
            log.error("保存指标数据失败: projectId={}, userId={}, metricLevel={}, timeDimension={}, snapshotDate={}", 
                     projectId, userId, metricLevel, timeDimension, snapshotDate, e);
            // 不抛出异常，避免影响其他项目的计算
        }
    }

    /**
     * 指标1: 用例平均CS复杂分
     */
    private BigDecimal calculateAvgCaseCS(Map<String, CaseMetricsDetail> csCacheMap) {
        try {
            if (csCacheMap == null || csCacheMap.isEmpty()) {
                return BigDecimal.ZERO;
            }
            
            BigDecimal totalCS = csCacheMap.values().stream()
                    .map(CaseMetricsDetail::getCsScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return totalCS.divide(BigDecimal.valueOf(csCacheMap.size()), 2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.error("计算平均CS失败", e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 指标2: 用例产出率
     */
    private BigDecimal calculateCaseOutputRate(String projectId, Long startTime, Long endTime,
                                              Map<String, CaseMetricsDetail> csCacheMap) {
        try {
            Long completedCount = extCaseMetricsMapper.countCasesByProjectAndTimeRange(projectId, startTime, endTime);
            if (completedCount == null || completedCount == 0) {
                return BigDecimal.ZERO;
            }
            
            BigDecimal totalCS = calculateTotalCSForPeriod(projectId, startTime, endTime, csCacheMap);
            if (totalCS.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO;
            }
            
            return BigDecimal.valueOf(completedCount)
                    .divide(totalCS, 4, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.error("计算用例产出率失败: projectId={}", projectId, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 指标3: 用例变更热度
     */
    private Integer calculateCaseChangeHeat(String projectId, Long startTime, Long endTime) {
        try {
            // 使用 case_change_log 表统计被修改的用例数
            Long modifiedCount = extCaseMetricsMapper.countModifiedCasesByChangeLog(projectId, null, startTime, endTime);
            return modifiedCount != null ? modifiedCount.intValue() : 0;
        } catch (Exception e) {
            log.error("计算用例变更热度失败: projectId={}", projectId, e);
            return 0;
        }
    }

    /**
     * 指标4: 高价值用例执行热度
     */
    private BigDecimal calculateHighValueExecRate(String projectId, Long startTime, Long endTime,
                                                  Map<String, CaseMetricsDetail> csCacheMap) {
        try {
            List<Map<String, Object>> highFreqCases = extCaseMetricsMapper.getHighFrequencyCases(
                    projectId, CaseMetricsConstants.HIGH_FREQ_THRESHOLD, startTime, endTime);
            
            if (highFreqCases == null || highFreqCases.isEmpty()) {
                return BigDecimal.ZERO;
            }
            
            BigDecimal highFreqExecCS = highFreqCases.stream()
                    .map(map -> {
                        Object csScore = map.get("cs_score");
                        Object execCount = map.get("exec_count");
                        if (csScore != null && execCount != null) {
                            return new BigDecimal(csScore.toString())
                                    .multiply(new BigDecimal(execCount.toString()));
                        }
                        return BigDecimal.ZERO;
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            List<Map<String, Object>> allExecCounts = loadExecutionCountsByProject(projectId, startTime, endTime);
            BigDecimal allExecCS = calculateExecutionCs(allExecCounts, csCacheMap);
            
            if (allExecCS.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO;
            }
            
            return highFreqExecCS.divide(allExecCS, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        } catch (Exception e) {
            log.error("计算高价值用例执行热度失败: projectId={}", projectId, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 指标5: 测试工时节约率
     */
    private BigDecimal calculateTimeSavingRate(String projectId, Long startTime, Long endTime) {
        try {
            // 使用 case_metrics_detail 表的 case_source_type 字段统计复用用例数（REUSE/COPY）
            Long reusedCount = extCaseMetricsMapper.countReusedCasesBySourceType(projectId, null, startTime, endTime);
            if (reusedCount == null || reusedCount == 0) {
                return BigDecimal.ZERO;
            }
            
            Long totalCount = extCaseMetricsMapper.countCasesByProject(projectId);
            if (totalCount == null || totalCount == 0) {
                return BigDecimal.ZERO;
            }
            
            return BigDecimal.valueOf(reusedCount)
                    .divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        } catch (Exception e) {
            log.error("计算测试工时节约率失败: projectId={}", projectId, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 指标6: 测试计划用例复用率
     */
    private BigDecimal calculatePlanCaseReuseRate(String projectId, Long startTime, Long endTime) {
        try {
            // 使用 case_metrics_detail 表的 case_source_type 字段统计复用用例数（REUSE/COPY）
            Long reusedCount = extCaseMetricsMapper.countReusedCasesBySourceType(projectId, null, startTime, endTime);
            Long totalCount = extCaseMetricsMapper.countCasesByProject(projectId);
            
            if (totalCount == null || totalCount == 0) {
                return BigDecimal.ZERO;
            }
            
            return BigDecimal.valueOf(reusedCount != null ? reusedCount : 0)
                    .divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        } catch (Exception e) {
            log.error("计算测试计划用例复用率失败: projectId={}", projectId, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 指标7: 测试计划用例修改率
     */
    private BigDecimal calculatePlanCaseModifyRate(String projectId, Long startTime, Long endTime) {
        try {
            // 使用 case_change_log 表统计被修改的用例数
            Long modifiedCount = extCaseMetricsMapper.countModifiedCasesByChangeLog(projectId, null, startTime, endTime);
            Long totalCount = extCaseMetricsMapper.countCasesByProject(projectId);
            
            if (totalCount == null || totalCount == 0) {
                return BigDecimal.ZERO;
            }
            
            return BigDecimal.valueOf(modifiedCount != null ? modifiedCount : 0)
                    .divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        } catch (Exception e) {
            log.error("计算测试计划用例修改率失败: projectId={}", projectId, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 指标8: 测试计划用例新增率
     */
    private BigDecimal calculatePlanCaseNewRate(String projectId, Long startTime, Long endTime) {
        try {
            Long newCount = extCaseMetricsMapper.countCasesByProjectAndTimeRange(projectId, startTime, endTime);
            Long totalCount = extCaseMetricsMapper.countCasesByProject(projectId);
            
            if (totalCount == null || totalCount == 0) {
                return BigDecimal.ZERO;
            }
            
            return BigDecimal.valueOf(newCount != null ? newCount : 0)
                    .divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        } catch (Exception e) {
            log.error("计算测试计划用例新增率失败: projectId={}", projectId, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 测试计划维度新增率/变更率（按计划组聚合）
     * 接口传计划 id，后端根据计划 id 查计划组 id，按计划组统计「本次新增用例数」「测试计划用例总数」「测试计划变更次数」并计算率
     */
    public PlanGroupMetricsDTO getPlanGroupMetrics(String planId, Long startTime, Long endTime) {
        PlanGroupMetricsDTO dto = new PlanGroupMetricsDTO();
        dto.setPlanId(planId);
        if (planId == null || planId.isEmpty()) {
            dto.setTotalCaseCount(0L);
            dto.setNewCaseCount(0L);
            dto.setModifiedCaseCount(0L);
            dto.setPlanGroupNewRate(BigDecimal.ZERO);
            dto.setPlanGroupModifyRate(BigDecimal.ZERO);
            return dto;
        }
        try {
            String groupId = extCaseMetricsMapper.getGroupIdByPlanId(planId);
            dto.setGroupId(groupId);
            if (groupId == null || groupId.isEmpty()) {
                dto.setTotalCaseCount(0L);
                dto.setNewCaseCount(0L);
                dto.setModifiedCaseCount(0L);
                dto.setPlanGroupNewRate(BigDecimal.ZERO);
                dto.setPlanGroupModifyRate(BigDecimal.ZERO);
                return dto;
            }
            Long total = extCaseMetricsMapper.countPlanGroupCaseTotal(groupId);
            Long newCount = extCaseMetricsMapper.countPlanGroupNewCases(groupId);
            Long modifiedCount = extCaseMetricsMapper.countPlanGroupModifiedCases(groupId, startTime, endTime);
            dto.setTotalCaseCount(total != null ? total : 0L);
            dto.setNewCaseCount(newCount != null ? newCount : 0L);
            dto.setModifiedCaseCount(modifiedCount != null ? modifiedCount : 0L);
            if (total != null && total > 0) {
                dto.setPlanGroupNewRate(BigDecimal.valueOf(dto.getNewCaseCount())
                        .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)));
                dto.setPlanGroupModifyRate(BigDecimal.valueOf(dto.getModifiedCaseCount())
                        .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)));
            } else {
                dto.setPlanGroupNewRate(BigDecimal.ZERO);
                dto.setPlanGroupModifyRate(BigDecimal.ZERO);
            }
        } catch (Exception e) {
            log.error("计算计划组指标失败: planId={}", planId, e);
            dto.setTotalCaseCount(0L);
            dto.setNewCaseCount(0L);
            dto.setModifiedCaseCount(0L);
            dto.setPlanGroupNewRate(BigDecimal.ZERO);
            dto.setPlanGroupModifyRate(BigDecimal.ZERO);
        }
        return dto;
    }

    /**
     * 指标9: 测试计划平均用例执行时长
     */
    private Long calculatePlanAvgExecDuration(CaseExecutionRecordAggregate summary) {
        if (summary == null || summary.getExecCount() == null || summary.getExecCount() == 0) {
            return 0L;
        }
        long totalDuration = summary.getTotalDuration() == null ? 0L : summary.getTotalDuration();
        return totalDuration / summary.getExecCount();
    }

    /**
     * 指标10: 测试计划用例通过率
     */
    private BigDecimal calculatePlanPassRate(CaseExecutionRecordAggregate summary) {
        if (summary == null || summary.getExecCount() == null || summary.getExecCount() == 0) {
            return BigDecimal.ZERO;
        }
        long passCount = summary.getPassCount() == null ? 0L : summary.getPassCount();
        return BigDecimal.valueOf(passCount)
                .divide(BigDecimal.valueOf(summary.getExecCount()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * 指标11: 测试计划首次通过率
     */
    private BigDecimal calculatePlanFirstPassRate(CaseExecutionRecordAggregate summary) {
        if (summary == null || summary.getFirstExecCount() == null || summary.getFirstExecCount() == 0) {
            return BigDecimal.ZERO;
        }
        long firstPassCount = summary.getFirstPassCount() == null ? 0L : summary.getFirstPassCount();
        return BigDecimal.valueOf(firstPassCount)
                .divide(BigDecimal.valueOf(summary.getFirstExecCount()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * 计算总CS分值
     */
    private BigDecimal calculateTotalCS(Map<String, CaseMetricsDetail> csCacheMap) {
        try {
            if (csCacheMap == null || csCacheMap.isEmpty()) {
                return BigDecimal.ZERO;
            }
            
            return csCacheMap.values().stream()
                    .map(CaseMetricsDetail::getCsScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            log.error("计算总CS分值失败", e);
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * 计算指定时间段的总CS分值
     */
    private BigDecimal calculateTotalCSForPeriod(String projectId, Long startTime, Long endTime,
                                                 Map<String, CaseMetricsDetail> csCacheMap) {
        try {
            if (csCacheMap == null || csCacheMap.isEmpty()) {
                return BigDecimal.ZERO;
            }
            List<String> caseIds = extCaseMetricsMapper.getCaseIdsByProject(projectId);
            if (caseIds == null || caseIds.isEmpty()) {
                return BigDecimal.ZERO;
            }
            
            return caseIds.stream()
                    .map(csCacheMap::get)
                    .filter(cache -> cache != null)
                    .map(CaseMetricsDetail::getCsScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            log.error("计算时间段总CS分值失败: projectId={}", projectId, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 加载项目级别的 CS 缓存并构建 map
     */
    private Map<String, CaseMetricsDetail> loadProjectCaseMetricsDetail(String projectId) {
        List<CaseMetricsDetail> csCaches = caseMetricsDetailMapper.selectByProjectId(projectId);
        if (csCaches == null || csCaches.isEmpty()) {
            return Collections.emptyMap();
        }
        return csCaches.stream()
                .filter(Objects::nonNull)
                .filter(cache -> cache.getCaseId() != null)
                .collect(Collectors.toMap(CaseMetricsDetail::getCaseId, cache -> cache, (existing, replacement) -> existing));
    }

    private Map<String, CaseMetricsDetail> loadCaseMetricsDetailByCaseIds(List<String> caseIds) {
        if (caseIds == null || caseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<CaseMetricsDetail> csCaches = caseMetricsDetailMapper.selectByProjectId(caseIds.get(0));
        if (csCaches == null || csCaches.isEmpty()) {
            return Collections.emptyMap();
        }
        return csCaches.stream()
                .filter(Objects::nonNull)
                .filter(cache -> cache.getCaseId() != null && caseIds.contains(cache.getCaseId()))
                .collect(Collectors.toMap(CaseMetricsDetail::getCaseId, cache -> cache, (existing, replacement) -> existing));
    }

    private CaseExecutionRecordAggregate loadExecutionSummaryByProject(String projectId, Long startTime, Long endTime) {
        try {
            CaseExecutionRecordAggregate aggregate = caseExecutionRecordMapper.selectExecutionSummaryByProject(
                    projectId, startTime, endTime);
            return aggregate != null ? aggregate : new CaseExecutionRecordAggregate();
        } catch (Exception e) {
            log.warn("加载项目执行记录汇总失败: projectId={}", projectId, e);
            return new CaseExecutionRecordAggregate();
        }
    }

    private CaseExecutionRecordAggregate loadExecutionSummaryByCaseIds(List<String> caseIds, Long startTime, Long endTime) {
        if (caseIds == null || caseIds.isEmpty()) {
            return new CaseExecutionRecordAggregate();
        }
        try {
            CaseExecutionRecordAggregate aggregate = caseExecutionRecordMapper.selectExecutionSummaryByCaseIds(
                    caseIds, startTime, endTime);
            return aggregate != null ? aggregate : new CaseExecutionRecordAggregate();
        } catch (Exception e) {
            log.warn("加载用例执行记录汇总失败", e);
            return new CaseExecutionRecordAggregate();
        }
    }

    private List<Map<String, Object>> loadExecutionCountsByProject(String projectId, Long startTime, Long endTime) {
        try {
            List<Map<String, Object>> result = caseExecutionRecordMapper.selectExecutionCountsByProject(projectId, startTime, endTime);
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.warn("加载项目执行次数统计失败: projectId={}", projectId, e);
            return Collections.emptyList();
        }
    }

    private List<Map<String, Object>> loadExecutionCountsByCaseIds(List<String> caseIds, Long startTime, Long endTime) {
        if (caseIds == null || caseIds.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> result = caseExecutionRecordMapper.selectExecutionCountsByCaseIds(caseIds, startTime, endTime);
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.warn("加载用例执行次数统计失败", e);
            return Collections.emptyList();
        }
    }

    private BigDecimal calculateExecutionCs(List<Map<String, Object>> executionCounts, Map<String, CaseMetricsDetail> csCacheMap) {
        if (executionCounts == null || executionCounts.isEmpty() || csCacheMap == null || csCacheMap.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return executionCounts.stream()
                .map(map -> {
                    Object execCountObj = map.get("exec_count");
                    Object caseIdObj = map.get("case_id");
                    if (execCountObj == null || caseIdObj == null) {
                        return BigDecimal.ZERO;
                    }
                    Number execCount = execCountObj instanceof Number ? (Number) execCountObj
                            : execCountObj instanceof String ? new BigDecimal(execCountObj.toString()) : null;

                    if (execCount == null) {
                        return BigDecimal.ZERO;
                    }

                    CaseMetricsDetail csCache = csCacheMap.get(caseIdObj.toString());
                    if (csCache == null || csCache.getCsScore() == null) {
                        return BigDecimal.ZERO;
                    }

                    return csCache.getCsScore().multiply(BigDecimal.valueOf(execCount.longValue()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 获取CS指标（支持多维度查询）
     */
    public CSMetricsDTO getCSMetrics(CSMetricsQueryRequest request) {
        log.info("查询CS指标: dimension={}, dimensionValue={}, startTime={}, endTime={}", 
                request.getDimension(), request.getDimensionValue(), 
                request.getStartTime(), request.getEndTime());
        
        String dimensionValue = request.getDimensionValue();
        Long startTime = request.getStartTime();
        Long endTime = request.getEndTime();
        
        CaseMetricsDTO caseMetrics = new CaseMetricsDTO();
        caseMetrics.setProjectId(dimensionValue);
        caseMetrics.setTimeRangeStart(startTime);
        caseMetrics.setTimeRangeEnd(endTime);
        
        switch (request.getDimension().toLowerCase()) {
            case "project":
                caseMetrics = getComprehensiveMetrics(createQueryRequest(dimensionValue, startTime, endTime));
                break;
            case "user":
                if ("all".equalsIgnoreCase(dimensionValue)) {
                    // 所有用户的汇总指标
                    caseMetrics = getAllUsersMetrics(startTime, endTime);
                } else {
                    // 单个用户的指标
                    caseMetrics = getComprehensiveMetricsByUser(dimensionValue, startTime, endTime);
                }
                break;
            case "team":
                log.warn("团队维度暂未实现");
                break;
            default:
                log.warn("不支持的维度类型: {}", request.getDimension());
        }
        
        return convertToCSMetricsDTO(caseMetrics);
    }

    /**
     * 计算单个用例的CS分值
     */
    public BigDecimal calculateCaseCS(String caseId) {
        CaseMetricsDetail detail = csCalculationService.getOrCalculateCSDetail(caseId);
        return detail != null ? detail.getCsScore() : BigDecimal.ZERO;
    }

    private CaseMetricsQueryRequest createQueryRequest(String projectId, Long startTime, Long endTime) {
        CaseMetricsQueryRequest request = new CaseMetricsQueryRequest();
        request.setProjectId(projectId);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        return request;
    }

    /**
     * 获取用户维度的综合指标（11个核心指标）
     */
    public CaseMetricsDTO getComprehensiveMetricsByUser(String userId, Long startTime, Long endTime) {
        log.info("查询用户维度综合指标: userId={}, startTime={}, endTime={}", 
                userId, startTime, endTime);
        
        CaseMetricsDTO dto = new CaseMetricsDTO();
        dto.setProjectId(userId);
        dto.setTimeRangeStart(startTime);
        dto.setTimeRangeEnd(endTime);
        
        try {
            List<String> caseIds = extCaseMetricsMapper.getCaseIdsByUser(userId);
            if (caseIds == null) {
                caseIds = Collections.emptyList();
            }
            Map<String, CaseMetricsDetail> csCacheMap = loadCaseMetricsDetailByCaseIds(caseIds);
            CaseExecutionRecordAggregate userExecutionSummary = loadExecutionSummaryByCaseIds(caseIds, startTime, endTime);

            dto.setAvgCaseCS(calculateAvgCaseCSByUser(userId, csCacheMap));
            
            dto.setCaseOutputRate(calculateCaseOutputRateByUser(userId, startTime, endTime, csCacheMap));
            
            dto.setCaseChangeHeat(calculateCaseChangeHeatByUser(userId, startTime, endTime));
            
            dto.setHighValueExecRate(calculateHighValueExecRateByUser(
                    userId, startTime, endTime, caseIds, csCacheMap));
            
            dto.setTimeSavingRate(calculateTimeSavingRateByUser(userId, startTime, endTime));
            
            dto.setPlanCaseReuseRate(calculatePlanCaseReuseRateByUser(userId, startTime, endTime));
            
            dto.setPlanCaseModifyRate(calculatePlanCaseModifyRateByUser(userId, startTime, endTime));
            
            dto.setPlanCaseNewRate(calculatePlanCaseNewRateByUser(userId, startTime, endTime));
            
            dto.setPlanAvgExecDuration(calculatePlanAvgExecDuration(userExecutionSummary));
            
            dto.setPlanPassRate(calculatePlanPassRate(userExecutionSummary));
            
            dto.setPlanFirstPassRate(calculatePlanFirstPassRate(userExecutionSummary));
            
            Long totalCount = extCaseMetricsMapper.countCasesByUser(userId);
            dto.setTotalCaseCount(totalCount != null ? totalCount.intValue() : 0);
            
            Long completedCount = extCaseMetricsMapper.countCasesByUserAndTimeRange(userId, startTime, endTime);
            dto.setCompletedCaseCount(completedCount != null ? completedCount.intValue() : 0);
            
            BigDecimal totalCS = calculateTotalCSByUser(userId, csCacheMap);
            dto.setTotalCS(totalCS);
            
            List<Map<String, Object>> highFreqCases = extCaseMetricsMapper.getHighFrequencyCasesByUser(
                    userId, CaseMetricsConstants.HIGH_FREQ_THRESHOLD, startTime, endTime);
            dto.setHighFreqCaseCount(highFreqCases != null ? highFreqCases.size() : 0);
            
        } catch (Exception e) {
            log.error("计算用户维度综合指标失败: userId={}", userId, e);
        }
        
        return dto;
    }

    /**
     * 获取所有用户的汇总指标（11个核心指标）
     */
    public CaseMetricsDTO getAllUsersMetrics(Long startTime, Long endTime) {
        log.info("查询所有用户汇总指标: startTime={}, endTime={}", startTime, endTime);
        
        CaseMetricsDTO dto = new CaseMetricsDTO();
        dto.setProjectId("all");
        dto.setTimeRangeStart(startTime);
        dto.setTimeRangeEnd(endTime);
        
        try {
            // 获取所有用户ID（通过查询所有用例的 create_user）
            List<String> allUserIds = extCaseMetricsMapper.getAllUserIds();
            if (allUserIds == null || allUserIds.isEmpty()) {
                log.warn("未找到任何用户");
                return dto;
            }
            
            log.info("找到 {} 个用户，开始计算汇总指标", allUserIds.size());
            
            // 汇总所有用户的数据
            long totalCaseCount = 0;
            BigDecimal totalCS = BigDecimal.ZERO;
            int totalCaseChangeHeat = 0;
            int totalHighFreqCaseCount = 0;
            int totalCompletedCaseCount = 0;
            
            BigDecimal sumAvgCaseCS = BigDecimal.ZERO;
            BigDecimal sumCaseOutputRate = BigDecimal.ZERO;
            BigDecimal sumHighValueExecRate = BigDecimal.ZERO;
            BigDecimal sumTimeSavingRate = BigDecimal.ZERO;
            BigDecimal sumPlanCaseReuseRate = BigDecimal.ZERO;
            BigDecimal sumPlanCaseModifyRate = BigDecimal.ZERO;
            BigDecimal sumPlanCaseNewRate = BigDecimal.ZERO;
            long sumPlanAvgExecDuration = 0;
            BigDecimal sumPlanPassRate = BigDecimal.ZERO;
            BigDecimal sumPlanFirstPassRate = BigDecimal.ZERO;
            
            int validUserCount = 0; // 有效用户数（有数据的用户）
            
            // 对每个用户计算指标并汇总
            for (String userId : allUserIds) {
                try {
                    CaseMetricsDTO userMetrics = getComprehensiveMetricsByUser(userId, startTime, endTime);
                    
                    // 计数类指标：累加
                    totalCaseCount += (userMetrics.getTotalCaseCount() != null ? userMetrics.getTotalCaseCount() : 0);
                    if (userMetrics.getTotalCS() != null) {
                        totalCS = totalCS.add(userMetrics.getTotalCS());
                    }
                    totalCaseChangeHeat += (userMetrics.getCaseChangeHeat() != null ? userMetrics.getCaseChangeHeat() : 0);
                    totalHighFreqCaseCount += (userMetrics.getHighFreqCaseCount() != null ? userMetrics.getHighFreqCaseCount() : 0);
                    totalCompletedCaseCount += (userMetrics.getCompletedCaseCount() != null ? userMetrics.getCompletedCaseCount() : 0);
                    
                    // 比率类指标：累加用于后续计算平均值
                    if (userMetrics.getAvgCaseCS() != null) {
                        sumAvgCaseCS = sumAvgCaseCS.add(userMetrics.getAvgCaseCS());
                    }
                    if (userMetrics.getCaseOutputRate() != null) {
                        sumCaseOutputRate = sumCaseOutputRate.add(userMetrics.getCaseOutputRate());
                    }
                    if (userMetrics.getHighValueExecRate() != null) {
                        sumHighValueExecRate = sumHighValueExecRate.add(userMetrics.getHighValueExecRate());
                    }
                    if (userMetrics.getTimeSavingRate() != null) {
                        sumTimeSavingRate = sumTimeSavingRate.add(userMetrics.getTimeSavingRate());
                    }
                    if (userMetrics.getPlanCaseReuseRate() != null) {
                        sumPlanCaseReuseRate = sumPlanCaseReuseRate.add(userMetrics.getPlanCaseReuseRate());
                    }
                    if (userMetrics.getPlanCaseModifyRate() != null) {
                        sumPlanCaseModifyRate = sumPlanCaseModifyRate.add(userMetrics.getPlanCaseModifyRate());
                    }
                    if (userMetrics.getPlanCaseNewRate() != null) {
                        sumPlanCaseNewRate = sumPlanCaseNewRate.add(userMetrics.getPlanCaseNewRate());
                    }
                    if (userMetrics.getPlanAvgExecDuration() != null) {
                        sumPlanAvgExecDuration += userMetrics.getPlanAvgExecDuration();
                    }
                    if (userMetrics.getPlanPassRate() != null) {
                        sumPlanPassRate = sumPlanPassRate.add(userMetrics.getPlanPassRate());
                    }
                    if (userMetrics.getPlanFirstPassRate() != null) {
                        sumPlanFirstPassRate = sumPlanFirstPassRate.add(userMetrics.getPlanFirstPassRate());
                    }
                    
                    validUserCount++;
                } catch (Exception e) {
                    log.warn("计算用户 {} 的指标失败，跳过", userId, e);
                }
            }
            
            // 设置计数类指标
            dto.setTotalCaseCount((int) totalCaseCount);
            dto.setTotalCS(totalCS);
            dto.setCaseChangeHeat(totalCaseChangeHeat);
            dto.setHighFreqCaseCount(totalHighFreqCaseCount);
            dto.setCompletedCaseCount(totalCompletedCaseCount);
            
            // 计算比率类指标的平均值
            if (validUserCount > 0) {
                dto.setAvgCaseCS(sumAvgCaseCS.divide(BigDecimal.valueOf(validUserCount), 2, RoundingMode.HALF_UP));
                dto.setCaseOutputRate(sumCaseOutputRate.divide(BigDecimal.valueOf(validUserCount), 4, RoundingMode.HALF_UP));
                dto.setHighValueExecRate(sumHighValueExecRate.divide(BigDecimal.valueOf(validUserCount), 4, RoundingMode.HALF_UP));
                dto.setTimeSavingRate(sumTimeSavingRate.divide(BigDecimal.valueOf(validUserCount), 4, RoundingMode.HALF_UP));
                dto.setPlanCaseReuseRate(sumPlanCaseReuseRate.divide(BigDecimal.valueOf(validUserCount), 4, RoundingMode.HALF_UP));
                dto.setPlanCaseModifyRate(sumPlanCaseModifyRate.divide(BigDecimal.valueOf(validUserCount), 4, RoundingMode.HALF_UP));
                dto.setPlanCaseNewRate(sumPlanCaseNewRate.divide(BigDecimal.valueOf(validUserCount), 4, RoundingMode.HALF_UP));
                dto.setPlanAvgExecDuration(validUserCount > 0 ? sumPlanAvgExecDuration / validUserCount : 0L);
                dto.setPlanPassRate(sumPlanPassRate.divide(BigDecimal.valueOf(validUserCount), 4, RoundingMode.HALF_UP));
                dto.setPlanFirstPassRate(sumPlanFirstPassRate.divide(BigDecimal.valueOf(validUserCount), 4, RoundingMode.HALF_UP));
            } else {
                dto.setAvgCaseCS(BigDecimal.ZERO);
                dto.setCaseOutputRate(BigDecimal.ZERO);
                dto.setHighValueExecRate(BigDecimal.ZERO);
                dto.setTimeSavingRate(BigDecimal.ZERO);
                dto.setPlanCaseReuseRate(BigDecimal.ZERO);
                dto.setPlanCaseModifyRate(BigDecimal.ZERO);
                dto.setPlanCaseNewRate(BigDecimal.ZERO);
                dto.setPlanAvgExecDuration(0L);
                dto.setPlanPassRate(BigDecimal.ZERO);
                dto.setPlanFirstPassRate(BigDecimal.ZERO);
            }
            
            log.info("所有用户汇总指标计算完成，有效用户数: {}", validUserCount);
            
        } catch (Exception e) {
            log.error("计算所有用户汇总指标失败", e);
        }
        
        return dto;
    }

    /**
     * 用户维度指标1: 用例平均CS复杂分
     */
    private BigDecimal calculateAvgCaseCSByUser(String userId, Map<String, CaseMetricsDetail> csCacheMap) {
        try {
            if (csCacheMap == null || csCacheMap.isEmpty()) {
                return BigDecimal.ZERO;
            }
            
            BigDecimal totalCS = csCacheMap.values().stream()
                    .map(CaseMetricsDetail::getCsScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            return totalCS.divide(BigDecimal.valueOf(csCacheMap.size()), 2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.error("计算用户平均CS失败: userId={}", userId, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 用户维度指标2: 用例产出率
     */
    private BigDecimal calculateCaseOutputRateByUser(String userId, Long startTime, Long endTime,
                                                    Map<String, CaseMetricsDetail> csCacheMap) {
        try {
            Long completedCount = extCaseMetricsMapper.countCasesByUserAndTimeRange(userId, startTime, endTime);
            if (completedCount == null || completedCount == 0) {
                return BigDecimal.ZERO;
            }
            
            BigDecimal totalCS = calculateTotalCSForPeriodByUser(userId, startTime, endTime, csCacheMap);
            if (totalCS.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO;
            }
            
            return BigDecimal.valueOf(completedCount)
                    .divide(totalCS, 4, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.error("计算用户用例产出率失败: userId={}", userId, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 用户维度指标3: 用例变更热度
     */
    private Integer calculateCaseChangeHeatByUser(String userId, Long startTime, Long endTime) {
        try {
            // 使用 case_change_log 表统计被修改的用例数
            Long modifiedCount = extCaseMetricsMapper.countModifiedCasesByChangeLog(null, userId, startTime, endTime);
            return modifiedCount != null ? modifiedCount.intValue() : 0;
        } catch (Exception e) {
            log.error("计算用户用例变更热度失败: userId={}", userId, e);
            return 0;
        }
    }

    /**
     * 用户维度指标4: 高价值用例执行热度
     */
    private BigDecimal calculateHighValueExecRateByUser(
            String userId, Long startTime, Long endTime,
            List<String> caseIds, Map<String, CaseMetricsDetail> csCacheMap) {
        try {
            List<Map<String, Object>> highFreqCases = extCaseMetricsMapper.getHighFrequencyCasesByUser(
                    userId, CaseMetricsConstants.HIGH_FREQ_THRESHOLD, startTime, endTime);
            
            if (highFreqCases == null || highFreqCases.isEmpty()) {
                return BigDecimal.ZERO;
            }
            
            BigDecimal highFreqExecCS = highFreqCases.stream()
                    .map(map -> {
                        Object csScore = map.get("cs_score");
                        Object execCount = map.get("exec_count");
                        if (csScore != null && execCount != null) {
                            return new BigDecimal(csScore.toString())
                                    .multiply(new BigDecimal(execCount.toString()));
                        }
                        return BigDecimal.ZERO;
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            List<Map<String, Object>> executionCounts = loadExecutionCountsByCaseIds(caseIds, startTime, endTime);
            BigDecimal allExecCS = calculateExecutionCs(executionCounts, csCacheMap);
            
            if (allExecCS.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO;
            }
            
            return highFreqExecCS.divide(allExecCS, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        } catch (Exception e) {
            log.error("计算用户高价值用例执行热度失败: userId={}", userId, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 用户维度指标5: 测试工时节约率
     */
    private BigDecimal calculateTimeSavingRateByUser(String userId, Long startTime, Long endTime) {
        try {
            // 使用 case_metrics_detail 表的 case_source_type 字段统计复用用例数（REUSE/COPY）
            Long reusedCount = extCaseMetricsMapper.countReusedCasesBySourceType(null, userId, startTime, endTime);
            if (reusedCount == null || reusedCount == 0) {
                return BigDecimal.ZERO;
            }
            
            Long totalCount = extCaseMetricsMapper.countCasesByUser(userId);
            if (totalCount == null || totalCount == 0) {
                return BigDecimal.ZERO;
            }
            
            return BigDecimal.valueOf(reusedCount)
                    .divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        } catch (Exception e) {
            log.error("计算用户测试工时节约率失败: userId={}", userId, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 用户维度指标6: 测试计划用例复用率
     */
    private BigDecimal calculatePlanCaseReuseRateByUser(String userId, Long startTime, Long endTime) {
        try {
            // 使用 case_metrics_detail 表的 case_source_type 字段统计复用用例数（REUSE/COPY）
            Long reusedCount = extCaseMetricsMapper.countReusedCasesBySourceType(null, userId, startTime, endTime);
            Long totalCount = extCaseMetricsMapper.countCasesByUser(userId);
            
            if (totalCount == null || totalCount == 0) {
                return BigDecimal.ZERO;
            }
            
            return BigDecimal.valueOf(reusedCount != null ? reusedCount : 0)
                    .divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        } catch (Exception e) {
            log.error("计算用户测试计划用例复用率失败: userId={}", userId, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 用户维度指标7: 测试计划用例修改率
     */
    private BigDecimal calculatePlanCaseModifyRateByUser(String userId, Long startTime, Long endTime) {
        try {
            // 使用 case_change_log 表统计被修改的用例数
            Long modifiedCount = extCaseMetricsMapper.countModifiedCasesByChangeLog(null, userId, startTime, endTime);
            Long totalCount = extCaseMetricsMapper.countCasesByUser(userId);
            
            if (totalCount == null || totalCount == 0) {
                return BigDecimal.ZERO;
            }
            
            return BigDecimal.valueOf(modifiedCount != null ? modifiedCount : 0)
                    .divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        } catch (Exception e) {
            log.error("计算用户测试计划用例修改率失败: userId={}", userId, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 用户维度指标8: 测试计划用例新增率
     */
    private BigDecimal calculatePlanCaseNewRateByUser(String userId, Long startTime, Long endTime) {
        try {
            Long newCount = extCaseMetricsMapper.countCasesByUserAndTimeRange(userId, startTime, endTime);
            Long totalCount = extCaseMetricsMapper.countCasesByUser(userId);
            
            if (totalCount == null || totalCount == 0) {
                return BigDecimal.ZERO;
            }
            
            return BigDecimal.valueOf(newCount != null ? newCount : 0)
                    .divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        } catch (Exception e) {
            log.error("计算用户测试计划用例新增率失败: userId={}", userId, e);
            return BigDecimal.ZERO;
        }
    }


    /**
     * 计算用户的总CS分值
     */
    private BigDecimal calculateTotalCSByUser(String userId, Map<String, CaseMetricsDetail> csCacheMap) {
        try {
            if (csCacheMap == null || csCacheMap.isEmpty()) {
                return BigDecimal.ZERO;
            }
            
            BigDecimal totalCS = csCacheMap.values().stream()
                    .map(CaseMetricsDetail::getCsScore)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            return totalCS;
        } catch (Exception e) {
            log.error("计算用户总CS失败: userId={}", userId, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 计算用户在指定时间段内的总CS分值
     */
    private BigDecimal calculateTotalCSForPeriodByUser(String userId, Long startTime, Long endTime,
                                                       Map<String, CaseMetricsDetail> csCacheMap) {
        try {
            if (csCacheMap == null || csCacheMap.isEmpty()) {
                return BigDecimal.ZERO;
            }
            
            BigDecimal totalCS = csCacheMap.values().stream()
                    .map(CaseMetricsDetail::getCsScore)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            return totalCS;
        } catch (Exception e) {
            log.error("计算用户时间段总CS失败: userId={}", userId, e);
            return BigDecimal.ZERO;
        }
    }

    private CSMetricsDTO convertToCSMetricsDTO(CaseMetricsDTO caseMetrics) {
        CSMetricsDTO dto = new CSMetricsDTO();
        dto.setAvgCaseCS(caseMetrics.getAvgCaseCS());
        dto.setCaseOutputRate(caseMetrics.getCaseOutputRate());
        dto.setCaseChangeHeat(caseMetrics.getCaseChangeHeat());
        dto.setHighValueExecRate(caseMetrics.getHighValueExecRate());
        dto.setTimeSavingRate(caseMetrics.getTimeSavingRate());
        dto.setPlanCaseReuseRate(caseMetrics.getPlanCaseReuseRate());
        dto.setPlanCaseModifyRate(caseMetrics.getPlanCaseModifyRate());
        dto.setPlanCaseNewRate(caseMetrics.getPlanCaseNewRate());
        dto.setPlanAvgExecDuration(caseMetrics.getPlanAvgExecDuration() != null 
                ? new BigDecimal(caseMetrics.getPlanAvgExecDuration()) 
                : BigDecimal.ZERO);
        dto.setPlanPassRate(caseMetrics.getPlanPassRate());
        dto.setPlanFirstPassRate(caseMetrics.getPlanFirstPassRate());
        dto.setTotalCaseCount(caseMetrics.getTotalCaseCount());
        dto.setTotalCS(caseMetrics.getTotalCS());
        dto.setHighFreqCaseCount(caseMetrics.getHighFreqCaseCount());
        dto.setCompletedCaseCount(caseMetrics.getCompletedCaseCount());
        return dto;
    }
    
    /**
     * 根据指标类型查询用例列表及其CS值
     * 重写版本：直接从 case_metrics_detail 表查询，确保数据一致性
     */
    public CaseListResponseDTO getCaseListByMetric(CaseListQueryRequest request) {
        CaseListResponseDTO response = new CaseListResponseDTO();
        response.setPageNum(request.getPageNum());
        response.setPageSize(request.getPageSize());
        
        Long startTime = request.getStartTime();
        Long endTime = request.getEndTime();
        if (startTime != null && endTime != null && startTime > endTime) {
            Long temp = startTime;
            startTime = endTime;
            endTime = temp;
        }
        
        // 直接从 case_metrics_detail 表查询，带分页
        List<CaseDetailWithCSDTO> caseList = extCaseMetricsMapper.getCaseListByMetricDirect(
            request.getMetricType(),
            request.getDimension(),
            request.getDimensionValue(),
            startTime,
            endTime,
            (request.getPageNum() - 1) * request.getPageSize(),
            request.getPageSize()
        );
        
        // 查询总数
        Long total = extCaseMetricsMapper.countCasesByMetricDirect(
            request.getMetricType(),
            request.getDimension(),
            request.getDimensionValue(),
            startTime,
            endTime
        );
        
        response.setCaseList(caseList == null ? List.of() : caseList);
        response.setTotal(total == null ? 0L : total);
        
        return response;
    }
    
    /**
     * 根据指标类型获取用例ID列表
     */
    private List<String> getCaseIdsByMetricType(CaseListQueryRequest request) {
        String metricType = request.getMetricType();
        String projectId = request.getProjectId();
        String dimension = request.getDimension();
        String dimensionValue = request.getDimensionValue();
        Long startTime = request.getStartTime();
        Long endTime = request.getEndTime();
        if (startTime != null && endTime != null && startTime > endTime) {
            Long temp = startTime;
            startTime = endTime;
            endTime = temp;
        }
        
        // 个人维度：优先使用按用户的查询
        if ("personal".equalsIgnoreCase(dimension)) {
            return getPersonalCaseIdsByMetricType(metricType, dimensionValue, startTime, endTime);
        }

        // 如果 projectId 为 null，表示查询所有项目
        if (projectId == null) {
            switch (metricType) {
                case "avgCaseCS":
                case "caseOutputRate":
                case "totalCaseCount":
                case "totalCS":
                case "completedCount":
                case "planCaseReuseRate":
                case "planCaseModifyRate":
                case "planCaseNewRate":
                case "planAvgExecDuration":
                case "planPassRate":
                case "planFirstPassRate":
                    return filterCaseIds(extCaseMetricsMapper.getAllCaseIds(), dimension, dimensionValue);
                    
                case "caseChangeHeat":
                    // 用例变更率：变更原因不为 COPY 的用例（项目不限、时间不限、不限两库）
                    return filterCaseIds(extCaseMetricsMapper.getModifiedCaseIdsFromChangeLog(null, null, null, null, null, null), dimension, dimensionValue);
                    
                case "highValueExecRate":
                case "highFreqCaseCount":
                    return filterCaseIds(extCaseMetricsMapper.getHighFrequencyCaseIds(null, 3, startTime, endTime), dimension, dimensionValue);
                    
                case "timeSavingRate":
                    return filterCaseIds(extCaseMetricsMapper.getReusedCaseIdsFromSourceType(null, null, null, startTime, endTime), dimension, dimensionValue);
                    
                default:
                    return filterCaseIds(extCaseMetricsMapper.getAllCaseIds(), dimension, dimensionValue);
            }
        }
        
        // projectId 不为 null，按项目查询
        switch (metricType) {
            case "avgCaseCS":
            case "caseOutputRate":
            case "totalCaseCount":
            case "totalCS":
            case "completedCaseCount":
                return filterCaseIds(extCaseMetricsMapper.getCaseIdsByProjectForBatch(projectId), dimension, dimensionValue);
                
            case "caseChangeHeat":
                return filterCaseIds(extCaseMetricsMapper.getModifiedCaseIdsFromChangeLog(projectId, null, null, null, null, null), dimension, dimensionValue);
                
            case "highValueExecRate":
            case "highFreqCaseCount":
                return filterCaseIds(extCaseMetricsMapper.getHighFrequencyCaseIds(projectId, 3, startTime, endTime), dimension, dimensionValue);
                
            case "timeSavingRate":
                return filterCaseIds(extCaseMetricsMapper.getReusedCaseIdsFromSourceType(projectId, null, null, startTime, endTime), dimension, dimensionValue);
                
            case "planCaseReuseRate":
            case "planCaseModifyRate":
            case "planCaseNewRate":
            case "planAvgExecDuration":
            case "planPassRate":
            case "planFirstPassRate":
                return filterCaseIds(extCaseMetricsMapper.getCaseIdsByProjectForBatch(projectId), dimension, dimensionValue);
                
            default:
                return filterCaseIds(extCaseMetricsMapper.getCaseIdsByProjectForBatch(projectId), dimension, dimensionValue);
        }
    }
    
    private List<String> filterCaseIds(List<String> caseIds, String dimension, String dimensionValue) {
        if (caseIds == null || caseIds.isEmpty()) {
            return caseIds;
        }

        if (!"personal".equalsIgnoreCase(dimension) || dimensionValue == null || dimensionValue.isBlank()
                || "all".equalsIgnoreCase(dimensionValue)) {
            return caseIds;
        }

        List<String> userCaseIds = extCaseMetricsMapper.getCaseIdsByUser(dimensionValue);
        if (userCaseIds == null || userCaseIds.isEmpty()) {
            return List.of();
        }

        return caseIds.stream()
                .filter(userCaseIds::contains)
                .toList();
    }

    /** 两库模块名列表，用于变更统计范围限定（测试计划内或两库内） */
    private static List<String> getTwoLibraryModuleNames() {
        return List.of(
                CaseMetricsConstants.TwoLibraryModuleName.TEMPLATE_LIBRARY,
                CaseMetricsConstants.TwoLibraryModuleName.REGRESSION_LIBRARY);
    }

    /**
     * 两库+时间范围内「无引用无执行」的新增用例数（用于用例维度新增率分子）
     * 无 test_plan_functional_case 且无 test_plan_case_metrics.exec_count &gt; 0
     */
    public long countTwoLibraryNewCasesWithoutRefOrExec(String projectId, Long startTime, Long endTime) {
        List<String> ids = extCaseMetricsMapper.getTwoLibraryNewCaseIdsWithoutRefOrExec(
                projectId, null, startTime, endTime, getTwoLibraryModuleNames());
        return ids != null ? ids.size() : 0L;
    }

    private List<String> getPersonalCaseIdsByMetricType(String metricType, String userId, Long startTime, Long endTime) {
        if (userId == null || userId.isBlank()) {
            return List.of();
        }

        List<String> caseIds;
        switch (metricType) {
            case "caseChangeHeat":
                caseIds = extCaseMetricsMapper.getModifiedCaseIdsFromChangeLog(null, null, userId, null, null, null);
                break;
            case "highValueExecRate":
            case "highFreqCaseCount":
                caseIds = extCaseMetricsMapper.getHighFrequencyCaseIdsByUser(userId, CaseMetricsConstants.HIGH_FREQ_THRESHOLD, startTime, endTime);
                break;
            case "timeSavingRate":
                caseIds = extCaseMetricsMapper.getReusedCaseIdsFromSourceType(null, null, userId, startTime, endTime);
                break;
            default:
                caseIds = extCaseMetricsMapper.getCaseIdsByUser(userId);
        }

        Set<String> creationFiltered = filterCaseIdsByCreationTime(caseIds, startTime, endTime);
        if (creationFiltered.isEmpty()) {
            return List.of();
        }
        return caseIds.stream()
                .filter(creationFiltered::contains)
                .toList();
    }

    private Set<String> filterCaseIdsByCreationTime(List<String> caseIds, Long startTime, Long endTime) {
        if (caseIds == null || caseIds.isEmpty()) {
            return Collections.emptySet();
        }

        try {
            // 使用 case_metrics_detail 表进行时间过滤，确保与统计数字一致
            List<String> filteredIds = extCaseMetricsMapper.filterCaseIdsByTimeRange(caseIds, startTime, endTime);
            if (filteredIds == null || filteredIds.isEmpty()) {
                return Collections.emptySet();
            }
            return new HashSet<>(filteredIds);
        } catch (Exception e) {
            // 如果 case_metrics_detail 表不存在，降级为返回空集合
            // 需要先运行 CS 批量计算来创建该表
            log.warn("Failed to filter by time range, case_metrics_detail table may not exist. Please run CS batch calculation first: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    private List<CaseDetailWithCSDTO> buildCaseDetailList(List<String> caseIds, Long startTime, Long endTime) {
        List<CaseDetailWithCSDTO> result = new ArrayList<>();
        
        for (String caseId : caseIds) {
            try {
                FunctionalCase functionalCase = functionalCaseMapper.selectByPrimaryKey(caseId);
                if (functionalCase == null || (functionalCase.getDeleted() != null && functionalCase.getDeleted())) {
                    continue;
                }
                FunctionalCaseBlob caseBlob = functionalCaseBlobMapper.selectByPrimaryKey(caseId);
                CaseMetricsDetail csCache = caseMetricsDetailMapper.selectByCaseId(caseId);
                
                CaseDetailWithCSDTO dto = new CaseDetailWithCSDTO();
                dto.setCaseId(functionalCase.getId());
                dto.setCaseNum(functionalCase.getNum() != null ? String.valueOf(functionalCase.getNum()) : null);
                dto.setCaseName(functionalCase.getName());
                dto.setProjectId(functionalCase.getProjectId());
                dto.setModuleId(functionalCase.getModuleId());
                dto.setCreateUser(functionalCase.getCreateUser());
                dto.setCreateTime(functionalCase.getCreateTime());
                dto.setUpdateTime(functionalCase.getUpdateTime());
                
                if (caseBlob != null) {
                    dto.setDescription(caseBlob.getDescription() != null 
                        ? new String(caseBlob.getDescription(), java.nio.charset.StandardCharsets.UTF_8) 
                        : null);
                    dto.setPrerequisite(caseBlob.getPrerequisite() != null 
                        ? new String(caseBlob.getPrerequisite(), java.nio.charset.StandardCharsets.UTF_8) 
                        : null);
                }
                
                if (csCache != null) {
                    dto.setCsScore(csCache.getCsScore());
                    // Note: CaseMetricsDetail doesn't have cognitiveScore, preconditionScore, stepDetailScore
                    dto.setCsFactorC1(csCache.getCsFactorC1());
                    dto.setCsFactorC2(csCache.getCsFactorC2());
                    dto.setCsFactorC3(csCache.getCsFactorC3());
                    dto.setCsFactorC4(csCache.getCsFactorC4());
                    dto.setCsFactorC5(csCache.getCsFactorC5());
                    dto.setCsFactorC6(csCache.getCsFactorC6());
                    // Note: CaseMetricsDetail uses csFactorC6, not csFactorC7
                }
                
                if (functionalCase.getModuleId() != null) {
                    var module = functionalCaseModuleMapper.selectByPrimaryKey(functionalCase.getModuleId());
                    if (module != null) {
                        dto.setModuleName(module.getName());
                    }
                }
                
                if (functionalCase.getCreateUser() != null) {
                    var user = userMapper.selectByPrimaryKey(functionalCase.getCreateUser());
                    if (user != null) {
                        dto.setCreateUserName(user.getName());
                    }
                }
                
                result.add(dto);
            } catch (Exception e) {
                log.warn("构建用例详情失败: caseId={}", caseId, e);
            }
        }
        
        return result;
    }

}

