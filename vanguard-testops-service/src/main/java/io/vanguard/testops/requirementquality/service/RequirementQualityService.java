package io.vanguard.testops.requirementquality.service;

import io.vanguard.testops.project.domain.Project;
import io.vanguard.testops.requirementquality.dto.RequirementQualityCaseExecutionRowDTO;
import io.vanguard.testops.requirementquality.dto.RequirementQualityCaseExecutionRowVO;
import io.vanguard.testops.requirementquality.dto.RequirementQualityFilterOptionsDTO;
import io.vanguard.testops.requirementquality.dto.RequirementQualityListItemDTO;
import io.vanguard.testops.requirementquality.dto.RequirementQualityListRowVO;
import io.vanguard.testops.requirementquality.dto.RequirementQualityDetailDTO;
import io.vanguard.testops.requirementquality.dto.RequirementQualityExecutorContributionDTO;
import io.vanguard.testops.requirementquality.dto.RequirementQualityExecutorContributionVO;
import io.vanguard.testops.requirementquality.dto.RequirementQualityExecutionTrendDTO;
import io.vanguard.testops.requirementquality.dto.RequirementQualityExecutionTrendVO;
import io.vanguard.testops.requirementquality.dto.RequirementQualityOverviewDTO;
import io.vanguard.testops.requirementquality.dto.RequirementQualityReasonDistributionDTO;
import io.vanguard.testops.requirementquality.dto.RequirementQualityReasonDistributionVO;
import io.vanguard.testops.requirementquality.dto.RequirementQualityBenefitMetricsDTO;
import io.vanguard.testops.requirementquality.dto.RequirementQualityBenefitMetricsVO;
import io.vanguard.testops.requirementquality.dto.RequirementQualityChangeHeatDTO;
import io.vanguard.testops.requirementquality.dto.RequirementQualityExecutionEfficiencyDTO;
import io.vanguard.testops.requirementquality.dto.RequirementQualityReuseMetricsDTO;
import io.vanguard.testops.requirementquality.dto.RequirementQualityReuseMetricsVO;
import io.vanguard.testops.requirementquality.dto.RequirementQualityWorkHourDeviationDTO;
import io.vanguard.testops.requirementquality.dto.RequirementQualityWorkHourByLevelVO;
import io.vanguard.testops.requirementquality.mapper.ExtRequirementQualityMapper;
import io.vanguard.testops.requirementquality.request.RequirementQualityListRequest;
import io.vanguard.testops.system.dto.sdk.OptionDTO;
import io.vanguard.testops.system.dto.user.UserExcludeOptionDTO;
import io.vanguard.testops.system.mapper.BaseProjectMapper;
import io.vanguard.testops.system.mapper.BaseUserMapper;
import io.vanguard.testops.system.dto.page.Pager;
import org.apache.commons.lang3.StringUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 需求质量视图 - 业务层
 * 列表/概览/详情数据由本模块聚合，数据来源：meego_story_stats、test_plan(feishu_story_id)、用例/执行表等
 */
@Service
public class RequirementQualityService {

    @Resource
    private ExtRequirementQualityMapper extRequirementQualityMapper;

    @Resource
    private BaseProjectMapper baseProjectMapper;

    @Resource
    private BaseUserMapper baseUserMapper;

    /**
     * 用 user id 或 email 查对应 name（负责人/执行人等展示用；create_user 可能存 id 或 email）
     */
    private java.util.Map<String, String> buildUserIdOrEmailToNameMap(List<String> idsOrEmails) {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        if (idsOrEmails == null || idsOrEmails.isEmpty()) {
            return map;
        }
        List<UserExcludeOptionDTO> users = baseUserMapper.selectUserOptionByIdOrEmail(idsOrEmails);
        if (users != null) {
            for (UserExcludeOptionDTO u : users) {
                if (StringUtils.isNotBlank(u.getId())) {
                    map.put(u.getId(), u.getName() != null ? u.getName() : u.getId());
                }
                if (StringUtils.isNotBlank(u.getEmail())) {
                    map.put(u.getEmail(), u.getName() != null ? u.getName() : u.getEmail());
                }
            }
        }
        return map;
    }

    /**
     * 分页查询需求质量列表（按系分 6.2 聚合 test_plan + 用例/执行数据）
     */
    /**
     * 解析项目筛选：优先 projectIds，为空时用 projectId 转单元素列表（兼容旧版）
     */
    private List<String> resolveProjectIds(RequirementQualityListRequest request) {
        List<String> ids = request.getProjectIds();
        if (ids != null && !ids.isEmpty()) {
            return ids;
        }
        if (request.getProjectId() != null && !request.getProjectId().isEmpty()) {
            return Collections.singletonList(request.getProjectId());
        }
        return null;
    }

    public Pager<List<RequirementQualityListItemDTO>> pageList(RequirementQualityListRequest request) {
        List<String> projectIds = resolveProjectIds(request);
        long offset = (long) (request.getCurrent() - 1) * request.getPageSize();
        List<RequirementQualityListRowVO> rows = extRequirementQualityMapper.selectRequirementQualityListPage(
                projectIds,
                request.getStatus(),
                request.getStoryIds(),
                request.getExecutionPeriodStart(),
                request.getExecutionPeriodEnd(),
                request.getSortBy(),
                request.getSortOrder(),
                offset,
                request.getPageSize());
        long total = extRequirementQualityMapper.countRequirementQualityList(
                projectIds,
                request.getStatus(),
                request.getStoryIds(),
                request.getExecutionPeriodStart(),
                request.getExecutionPeriodEnd());

        List<String> ownerIds = rows.stream()
                .map(RequirementQualityListRowVO::getOwner)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        java.util.Map<String, String> ownerNameMap = buildUserIdOrEmailToNameMap(ownerIds);

        List<RequirementQualityListItemDTO> list = new ArrayList<>();
        for (RequirementQualityListRowVO row : rows) {
            RequirementQualityListItemDTO dto = new RequirementQualityListItemDTO();
            dto.setStoryId(row.getStoryId());
            dto.setStoryName(row.getStoryName());
            String ownerId = row.getOwner();
            dto.setOwner(StringUtils.isNotBlank(ownerId) ? ownerNameMap.getOrDefault(ownerId, ownerId) : null);
            dto.setStatus(row.getPlanStatus());
            dto.setProjectId(null);
            dto.setProjectName(null);
            dto.setIterationId(null);
            dto.setIterationName(null);
            dto.setCaseTotalCount(row.getCaseTotalCount() != null ? row.getCaseTotalCount() : 0L);
            dto.setCaseExecutedCount(row.getCaseExecutedCount() != null ? row.getCaseExecutedCount() : 0L);
            long totalCount = dto.getCaseTotalCount();
            long executedCount = dto.getCaseExecutedCount() != null ? dto.getCaseExecutedCount() : 0L;
            if (totalCount > 0) {
                double execRate = 100.0 * executedCount / totalCount;
                long passed = row.getPassedCount() != null ? row.getPassedCount() : 0L;
                double passRateVal = 100.0 * passed / totalCount;
                dto.setExecutionRate(Math.round(execRate * 100.0) / 100.0);
                dto.setPassRate(Math.round(passRateVal * 100.0) / 100.0);
            } else {
                dto.setExecutionRate(0.0);
                dto.setPassRate(0.0);
            }
            long firstPass = row.getFirstPassCount() != null ? row.getFirstPassCount() : 0L;
            long totalFromCer = row.getTotalExecutedCountFromCer() != null ? row.getTotalExecutedCountFromCer() : 0L;
            if (totalFromCer > 0) {
                double firstPassRateVal = 100.0 * firstPass / totalFromCer;
                dto.setFirstPassRate(Math.round(firstPassRateVal * 100.0) / 100.0);
            } else {
                dto.setFirstPassRate(0.0);
            }
            dto.setExecutionPeriodStart(row.getExecutionPeriodStart());
            dto.setExecutionPeriodEnd(row.getExecutionPeriodEnd());
            dto.setAvgWriteDeviationRate(row.getAvgWriteDeviationRate() != null ? row.getAvgWriteDeviationRate().doubleValue() : null);
            dto.setAvgExecDeviationRate(row.getAvgExecDeviationRate() != null ? row.getAvgExecDeviationRate().doubleValue() : null);
            dto.setDefectCount(row.getDefectCount());
            dto.setReopenRate(row.getReopenRate() != null ? row.getReopenRate().doubleValue() : null);
            dto.setCodeCoverage(row.getCodeCoverage() != null ? row.getCodeCoverage().doubleValue() : null);
            fillDefectAndDeployRates(row, dto);
            list.add(dto);
        }
        return new Pager<>(list, total, request.getPageSize(), request.getCurrent());
    }

    /**
     * 根据列表行原始数据计算并填充：前端缺陷率、后端缺陷率、总千行代码缺陷率、变更失败率、变更成功率；代码覆盖率直接取库已在调用前设置。
     * 变更成功率 = (成功发布数 / 总发布数) × 100% = (deploy_total_count - deploy_failure_count) / deploy_total_count × 100。
     */
    private void fillDefectAndDeployRates(RequirementQualityListRowVO row, RequirementQualityListItemDTO dto) {
        Integer frontLoc = row.getFrontendLocChanged() != null ? row.getFrontendLocChanged() : 0;
        Integer backLoc = row.getBackendLocChanged() != null ? row.getBackendLocChanged() : 0;
        int frontDefect = row.getFrontendDefectCount() != null ? row.getFrontendDefectCount() : 0;
        int backDefect = row.getBackendDefectCount() != null ? row.getBackendDefectCount() : 0;
        if (frontLoc > 0) {
            dto.setFrontendDefectRate(Math.round(1000.0 * frontDefect / frontLoc * 100.0) / 100.0);
        } else {
            dto.setFrontendDefectRate(null);
        }
        if (backLoc > 0) {
            dto.setBackendDefectRate(Math.round(1000.0 * backDefect / backLoc * 100.0) / 100.0);
        } else {
            dto.setBackendDefectRate(null);
        }
        long totalLoc = (long) frontLoc + backLoc;
        if (totalLoc > 0) {
            dto.setTotalDefectRatePer1k(Math.round(1000.0 * (frontDefect + backDefect) / totalLoc * 100.0) / 100.0);
        } else {
            dto.setTotalDefectRatePer1k(null);
        }
        dto.setChangeFailureRate(row.getChangeFailureRate() != null ? row.getChangeFailureRate().doubleValue() : null);
        Integer deployTotal = row.getDeployTotalCount() != null ? row.getDeployTotalCount() : 0;
        Integer deployFail = row.getDeployFailureCount() != null ? row.getDeployFailureCount() : 0;
        dto.setDeployTotalCount(deployTotal);
        dto.setDeployFailureCount(deployFail);
        if (deployTotal != null && deployTotal > 0) {
            dto.setChangeSuccessRate(Math.round(100.0 * (deployTotal - deployFail) / deployTotal * 100.0) / 100.0);
        } else {
            dto.setChangeSuccessRate(null);
        }
    }

    /** 详情页：根据列表行原始数据填充缺陷率与发布率指标 */
    private void fillDefectAndDeployRatesForDetail(RequirementQualityListRowVO row, RequirementQualityDetailDTO dto) {
        Integer frontLoc = row.getFrontendLocChanged() != null ? row.getFrontendLocChanged() : 0;
        Integer backLoc = row.getBackendLocChanged() != null ? row.getBackendLocChanged() : 0;
        int frontDefect = row.getFrontendDefectCount() != null ? row.getFrontendDefectCount() : 0;
        int backDefect = row.getBackendDefectCount() != null ? row.getBackendDefectCount() : 0;
        if (frontLoc > 0) {
            dto.setFrontendDefectRate(Math.round(1000.0 * frontDefect / frontLoc * 100.0) / 100.0);
        } else {
            dto.setFrontendDefectRate(null);
        }
        if (backLoc > 0) {
            dto.setBackendDefectRate(Math.round(1000.0 * backDefect / backLoc * 100.0) / 100.0);
        } else {
            dto.setBackendDefectRate(null);
        }
        long totalLoc = (long) frontLoc + backLoc;
        if (totalLoc > 0) {
            dto.setTotalDefectRatePer1k(Math.round(1000.0 * (frontDefect + backDefect) / totalLoc * 100.0) / 100.0);
        } else {
            dto.setTotalDefectRatePer1k(null);
        }
        dto.setChangeFailureRate(row.getChangeFailureRate() != null ? row.getChangeFailureRate().doubleValue() : null);
        Integer deployTotal = row.getDeployTotalCount() != null ? row.getDeployTotalCount() : 0;
        Integer deployFail = row.getDeployFailureCount() != null ? row.getDeployFailureCount() : 0;
        dto.setDeployTotalCount(deployTotal);
        dto.setDeployFailureCount(deployFail);
        if (deployTotal != null && deployTotal > 0) {
            dto.setChangeSuccessRate(Math.round(100.0 * (deployTotal - deployFail) / deployTotal * 100.0) / 100.0);
        } else {
            dto.setChangeSuccessRate(null);
        }
    }

    /**
     * 概览卡：本期需求数、总用例/已执行/执行率/通过率（需求→测试计划→用例 全局聚合）、平均工时偏差（流水线留空）
     */
    public RequirementQualityOverviewDTO getOverview(RequirementQualityListRequest request) {
        List<String> projectIds = resolveProjectIds(request);
        long total = extRequirementQualityMapper.countRequirementQualityList(
                projectIds,
                request.getStatus(),
                request.getStoryIds(),
                request.getExecutionPeriodStart(),
                request.getExecutionPeriodEnd());
        RequirementQualityOverviewDTO dto = new RequirementQualityOverviewDTO();
        dto.setRequirementTotal(total);
        dto.setAvgWriteDeviationRate(null);
        dto.setAvgExecDeviationRate(null);

        var agg = extRequirementQualityMapper.selectRequirementQualityOverviewAgg(
                projectIds,
                request.getStatus(),
                request.getStoryIds(),
                request.getExecutionPeriodStart(),
                request.getExecutionPeriodEnd());
        long caseTotal = agg != null && agg.getCaseTotalCount() != null ? agg.getCaseTotalCount() : 0L;
        long caseExecuted = agg != null && agg.getCaseExecutedCount() != null ? agg.getCaseExecutedCount() : 0L;
        long passed = agg != null && agg.getPassedCount() != null ? agg.getPassedCount() : 0L;
        dto.setCaseTotalCount(caseTotal);
        dto.setCaseExecutedCount(caseExecuted);
        if (caseTotal > 0) {
            double execRate = 100.0 * caseExecuted / caseTotal;
            double passRateVal = 100.0 * passed / caseTotal;
            dto.setExecutionRate(Math.round(execRate * 100.0) / 100.0);
            dto.setPassRate(Math.round(passRateVal * 100.0) / 100.0);
        } else {
            dto.setExecutionRate(0.0);
            dto.setPassRate(0.0);
        }
        Double avgFirstPass = agg != null ? agg.getAvgFirstPassRate() : null;
        if (avgFirstPass != null) {
            dto.setFirstPassRate(BigDecimal.valueOf(avgFirstPass).setScale(2, RoundingMode.HALF_UP).doubleValue());
        } else {
            dto.setFirstPassRate(null);
        }
        Double avgDefect = agg != null ? agg.getAvgDefectRatePer1k() : null;
        if (avgDefect != null) {
            dto.setAvgDefectRatePer1k(BigDecimal.valueOf(avgDefect).setScale(2, RoundingMode.HALF_UP).doubleValue());
        } else {
            dto.setAvgDefectRatePer1k(null);
        }
        return dto;
    }

    /**
     * 需求质量详情（当前仅返回 6.4.1 概览块，综合质量/变更热度等后续补充）
     */
    public RequirementQualityDetailDTO getDetail(String storyId) {
        List<RequirementQualityListRowVO> rows = extRequirementQualityMapper.selectRequirementQualityListPage(
                null, null, java.util.Collections.singletonList(storyId), null, null, null, null, 0L, 1);
        RequirementQualityDetailDTO dto = new RequirementQualityDetailDTO();
        dto.setStoryId(storyId);
        if (rows.isEmpty()) {
            dto.setStoryName(null);
            dto.setCaseTotalCount(0L);
            dto.setCaseExecutedCount(0L);
            dto.setExecutionRate(0.0);
            dto.setPassRate(0.0);
            return dto;
        }
        RequirementQualityListRowVO row = rows.get(0);
        dto.setStoryName(row.getStoryName());
        String ownerId = row.getOwner();
        if (StringUtils.isNotBlank(ownerId)) {
            java.util.Map<String, String> ownerNameMap = buildUserIdOrEmailToNameMap(Collections.singletonList(ownerId));
            dto.setOwner(ownerNameMap.getOrDefault(ownerId, ownerId));
        } else {
            dto.setOwner(null);
        }
        dto.setStatus(row.getPlanStatus());
        dto.setExecutionPeriodStart(row.getExecutionPeriodStart());
        dto.setExecutionPeriodEnd(row.getExecutionPeriodEnd());
        dto.setProjectId(null);
        dto.setProjectName(null);
        dto.setIterationId(null);
        dto.setIterationName(null);
        dto.setCodeCoverage(row.getCodeCoverage() != null ? row.getCodeCoverage().doubleValue() : null);
        dto.setFrontendDefectCount(row.getFrontendDefectCount());
        dto.setBackendDefectCount(row.getBackendDefectCount());
        dto.setFrontendLocChanged(row.getFrontendLocChanged());
        dto.setBackendLocChanged(row.getBackendLocChanged());
        fillDefectAndDeployRatesForDetail(row, dto);
        List<RequirementQualityCaseExecutionRowVO> caseRows = extRequirementQualityMapper.selectCaseExecutionDetailByStoryId(storyId);
        dto.setCaseExecutionList(mapCaseExecutionRows(caseRows));
        long total = caseRows != null ? caseRows.size() : 0L;
        long executed = 0L;
        long passed = 0L;
        if (caseRows != null && !caseRows.isEmpty()) {
            for (RequirementQualityCaseExecutionRowVO r : caseRows) {
                String last = StringUtils.isNotBlank(r.getLastExecResultMetrics()) ? r.getLastExecResultMetrics() : r.getLastExecResultTpfc();
                if (StringUtils.isNotBlank(last) && !"PENDING".equalsIgnoreCase(last)) {
                    executed++;
                    if (SUCCESS_RESULTS.contains(last.toUpperCase())) {
                        passed++;
                    }
                }
            }
        }
        dto.setCaseTotalCount(total);
        dto.setCaseExecutedCount(executed);
        if (total > 0) {
            dto.setExecutionRate(Math.round(100.0 * executed / total * 100.0) / 100.0);
            dto.setPassRate(Math.round(100.0 * passed / total * 100.0) / 100.0);
        } else {
            dto.setExecutionRate(0.0);
            dto.setPassRate(0.0);
        }

        List<RequirementQualityExecutorContributionVO> contribVos = extRequirementQualityMapper.selectExecutorContributionByStoryId(storyId);
        List<RequirementQualityExecutorContributionDTO> contribList = new ArrayList<>();
        if (contribVos != null && !contribVos.isEmpty()) {
            List<String> executorIds = contribVos.stream()
                    .map(RequirementQualityExecutorContributionVO::getExecutorId)
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .collect(Collectors.toList());
            java.util.Map<String, String> executorNameMap = buildUserIdOrEmailToNameMap(executorIds);
            for (RequirementQualityExecutorContributionVO vo : contribVos) {
                RequirementQualityExecutorContributionDTO cd = new RequirementQualityExecutorContributionDTO();
                cd.setExecutorId(vo.getExecutorId());
                cd.setExecutorName(StringUtils.isNotBlank(vo.getExecutorId()) ? executorNameMap.getOrDefault(vo.getExecutorId(), vo.getExecutorId()) : null);
                cd.setCaseCount(vo.getCaseCount() != null ? vo.getCaseCount() : 0L);
                contribList.add(cd);
            }
        }
        dto.setExecutorContributionList(contribList);

        List<RequirementQualityReasonDistributionVO> blockVos = extRequirementQualityMapper.selectBlockReasonDistributionByStoryId(storyId);
        dto.setBlockReasonDistribution(mapReasonDistribution(blockVos, BLOCK_REASON_NAME_MAP));

        List<RequirementQualityReasonDistributionVO> changeVos = extRequirementQualityMapper.selectChangeReasonDistributionByStoryId(storyId);
        dto.setChangeReasonDistribution(mapReasonDistribution(changeVos, CHANGE_REASON_NAME_MAP));

        List<RequirementQualityExecutionTrendVO> trendVos = extRequirementQualityMapper.selectExecutionTrendByStoryId(storyId);
        dto.setExecutionTrendList(mapExecutionTrend(trendVos));

        List<RequirementQualityReasonDistributionVO> priorityVos = extRequirementQualityMapper.selectPriorityDistributionByStoryId(storyId);
        dto.setPriorityDistribution(mapReasonDistribution(priorityVos, PRIORITY_NAME_MAP));

        RequirementQualityReuseMetricsVO reuseVo = extRequirementQualityMapper.selectReuseMetricsByStoryId(storyId);
        dto.setReuseMetrics(mapReuseMetrics(reuseVo));

        List<Map<String, Object>> levelStats = extRequirementQualityMapper.selectExpectedTimeByLevelByStoryId(storyId);
        Long actualWriteMin = extRequirementQualityMapper.selectActualWriteMinutesByStoryId(storyId);
        Long actualExecMs = extRequirementQualityMapper.selectActualExecMsByStoryId(storyId);
        RequirementQualityWorkHourByLevelVO workHourByLevel = mapWorkHourByLevel(levelStats, actualWriteMin, actualExecMs);
        dto.setWorkHourByLevel(workHourByLevel);
        dto.setWorkHourDeviation(mapWorkHourDeviation(workHourByLevel));

        RequirementQualityBenefitMetricsVO benefitVo = extRequirementQualityMapper.selectBenefitMetricsByStoryId(storyId);
        Map<String, Object> benefitUqsRaw = extRequirementQualityMapper.selectBenefitUqsRawByStoryId(storyId);
        dto.setBenefitMetrics(mapBenefitMetrics(benefitVo, dto.getReuseMetrics(), benefitUqsRaw));

        Map<String, Object> changeHeatRaw = extRequirementQualityMapper.selectChangeHeatRawByStoryId(storyId);
        dto.setChangeHeatMetrics(mapChangeHeatMetrics(changeHeatRaw));

        Map<String, Object> execEffRaw = extRequirementQualityMapper.selectExecutionEfficiencyRawByStoryId(storyId);
        dto.setExecutionEfficiencyMetrics(mapExecutionEfficiencyMetrics(execEffRaw));

        return dto;
    }

    /**
     * 变更热度指标：与效能大屏一致——用例新增率=新增数/期初数×100%，用例变更热度=修正数/总用例数×100%
     */
    private RequirementQualityChangeHeatDTO mapChangeHeatMetrics(Map<String, Object> raw) {
        RequirementQualityChangeHeatDTO dto = new RequirementQualityChangeHeatDTO();
        dto.setCaseIncreaseRate(0.0);
        dto.setNewCases(0L);
        dto.setExistingCases(0L);
        dto.setCaseChangeHeat(0.0);
        dto.setModifiedCases(0L);
        dto.setTotalCases(0L);
        if (raw == null) return dto;
        Long newCount = toLong(raw.get("new_case_count"));
        Long totalCount = toLong(raw.get("total_case_count"));
        Long modifiedCount = toLong(raw.get("modified_case_count"));
        dto.setNewCases(newCount != null ? newCount : 0L);
        dto.setExistingCases(totalCount != null ? totalCount : 0L);
        dto.setModifiedCases(modifiedCount != null ? modifiedCount : 0L);
        dto.setTotalCases(totalCount != null ? totalCount : 0L);
        if (totalCount != null && totalCount > 0) {
            dto.setCaseIncreaseRate(new BigDecimal(newCount != null ? newCount : 0)
                    .divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue());
            dto.setCaseChangeHeat(BigDecimal.valueOf(modifiedCount != null ? modifiedCount : 0)
                    .divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue());
        }
        dto.setCaseIncreaseRate(Math.round((dto.getCaseIncreaseRate() != null ? dto.getCaseIncreaseRate() : 0) * 100.0) / 100.0);
        dto.setCaseChangeHeat(Math.round((dto.getCaseChangeHeat() != null ? dto.getCaseChangeHeat() : 0) * 100.0) / 100.0);
        return dto;
    }

    /**
     * 执行效率指标：与效能大屏一致——平均执行时长=总时长(ms)/执行次数 转分钟，手动执行热度=高频CS总分/总CS总分×100%
     */
    private RequirementQualityExecutionEfficiencyDTO mapExecutionEfficiencyMetrics(Map<String, Object> raw) {
        RequirementQualityExecutionEfficiencyDTO dto = new RequirementQualityExecutionEfficiencyDTO();
        dto.setAvgExecutionTime(0.0);
        dto.setTotalExecutionTime(0.0);
        dto.setExecutionCount(0L);
        dto.setManualExecutionHeat(0.0);
        dto.setHighFreqRegressionScore(0.0);
        dto.setTotalCaseScore(0.0);
        if (raw == null) return dto;
        Long totalDurationMs = toLong(raw.get("total_duration_ms"));
        Long totalExecCount = toLong(raw.get("total_exec_count"));
        Object highFreqObj = raw.get("high_freq_cs_total");
        Object allCsObj = raw.get("all_exec_cs_total");
        long durationMs = totalDurationMs != null ? totalDurationMs : 0L;
        long execCount = totalExecCount != null ? totalExecCount : 0L;
        dto.setExecutionCount(execCount);
        double totalMin = durationMs / (1000.0 * 60.0);
        dto.setTotalExecutionTime(Math.round(totalMin * 100.0) / 100.0);
        if (execCount > 0) {
            dto.setAvgExecutionTime(Math.round(totalMin / execCount * 100.0) / 100.0);
        }
        double highFreq = 0.0;
        double allCs = 0.0;
        if (highFreqObj instanceof Number) highFreq = ((Number) highFreqObj).doubleValue();
        if (allCsObj instanceof Number) allCs = ((Number) allCsObj).doubleValue();
        dto.setHighFreqRegressionScore(Math.round(highFreq * 100.0) / 100.0);
        dto.setTotalCaseScore(Math.round(allCs * 100.0) / 100.0);
        if (allCs > 0) {
            dto.setManualExecutionHeat(Math.round(100.0 * highFreq / allCs * 100.0) / 100.0);
        }
        return dto;
    }

    private RequirementQualityWorkHourDeviationDTO mapWorkHourDeviation(RequirementQualityWorkHourByLevelVO workHourByLevel) {
        RequirementQualityWorkHourDeviationDTO dev = new RequirementQualityWorkHourDeviationDTO();
        if (workHourByLevel == null) {
            dev.setWritingDeviationRate(0.0);
            dev.setActualWritingHours(0.0);
            dev.setTheoreticalWritingHours(0.0);
            dev.setExecutionDeviationRate(0.0);
            dev.setActualExecutionMinutes(0.0);
            dev.setTheoreticalExecutionMinutes(0.0);
            return dev;
        }
        double actualWriteMin = workHourByLevel.getActualWriteMinutesTotal() != null ? workHourByLevel.getActualWriteMinutesTotal().doubleValue() : 0.0;
        double actualExecMin = workHourByLevel.getActualExecMinutesTotal() != null ? workHourByLevel.getActualExecMinutesTotal().doubleValue() : 0.0;
        double theoreticalWriteMin = sum(workHourByLevel.getExpectedWriteMinutesL1(), workHourByLevel.getExpectedWriteMinutesL2(),
                workHourByLevel.getExpectedWriteMinutesL3(), workHourByLevel.getExpectedWriteMinutesL4());
        double theoreticalExecMin = sum(workHourByLevel.getExpectedExecMinutesL1(), workHourByLevel.getExpectedExecMinutesL2(),
                workHourByLevel.getExpectedExecMinutesL3(), workHourByLevel.getExpectedExecMinutesL4());

        double actualWriteHours = actualWriteMin / 60.0;
        double theoreticalWriteHours = theoreticalWriteMin / 60.0;
        dev.setActualWritingHours(Math.round(actualWriteHours * 100.0) / 100.0);
        dev.setTheoreticalWritingHours(Math.round(theoreticalWriteHours * 100.0) / 100.0);
        dev.setWritingDeviationRate(theoreticalWriteHours > 0
                ? Math.round(Math.abs(actualWriteHours - theoreticalWriteHours) / theoreticalWriteHours * 10000.0) / 100.0
                : 0.0);

        dev.setActualExecutionMinutes(Math.round(actualExecMin * 100.0) / 100.0);
        dev.setTheoreticalExecutionMinutes(Math.round(theoreticalExecMin * 100.0) / 100.0);
        dev.setExecutionDeviationRate(theoreticalExecMin > 0
                ? Math.round(Math.abs(actualExecMin - theoreticalExecMin) / theoreticalExecMin * 10000.0) / 100.0
                : 0.0);
        return dev;
    }

    private static double sum(java.math.BigDecimal a, java.math.BigDecimal b, java.math.BigDecimal c, java.math.BigDecimal d) {
        double sum = 0.0;
        if (a != null) sum += a.doubleValue();
        if (b != null) sum += b.doubleValue();
        if (c != null) sum += c.doubleValue();
        if (d != null) sum += d.doubleValue();
        return sum;
    }

    /**
     * 其它效益指标：UQS 与效能大屏统一为 0.4×验证发现率 + 0.3×可执行率 + 0.3×复用率；首次通过率从 case_execution_record 取数。
     */
    private RequirementQualityBenefitMetricsDTO mapBenefitMetrics(RequirementQualityBenefitMetricsVO vo,
                                                                  RequirementQualityReuseMetricsDTO reuseMetrics,
                                                                  Map<String, Object> uqsRaw) {
        RequirementQualityBenefitMetricsDTO dto = new RequirementQualityBenefitMetricsDTO();
        dto.setAvgUQSScore(0.0);
        dto.setVerificationDiscoveryRate(0.0);
        dto.setExecutabilityRate(0.0);
        double reuseRate = reuseMetrics != null && reuseMetrics.getReuseRateByWorkload() != null ? reuseMetrics.getReuseRateByWorkload() : 0.0;
        dto.setReuseRate(reuseRate);
        dto.setFirstPassRate(0.0);
        dto.setFirstPassCount(0L);
        dto.setTotalExecutionCount(0L);

        if (vo != null) {
            long total = vo.getTotalExecutedCount() != null ? vo.getTotalExecutedCount() : 0L;
            long firstPass = vo.getFirstPassCount() != null ? vo.getFirstPassCount() : 0L;
            dto.setFirstPassCount(firstPass);
            dto.setTotalExecutionCount(total);
            dto.setFirstPassRate(total > 0 ? Math.round(100.0 * firstPass / total * 100.0) / 100.0 : 0.0);
        }

        Long totalExecCasesL = toLong(uqsRaw != null ? uqsRaw.get("total_exec_cases") : null);
        Long totalDefectsL = toLong(uqsRaw != null ? uqsRaw.get("total_defects") : null);
        Long totalExecCountL = toLong(uqsRaw != null ? uqsRaw.get("total_exec_count") : null);
        Long successWithoutBlockL = toLong(uqsRaw != null ? uqsRaw.get("success_without_block_count") : null);
        long totalExecCases = totalExecCasesL != null ? totalExecCasesL : 0L;
        long totalDefects = totalDefectsL != null ? totalDefectsL : 0L;
        long totalExecCount = totalExecCountL != null ? totalExecCountL : 0L;
        long successWithoutBlock = successWithoutBlockL != null ? successWithoutBlockL : 0L;

        double defectDiscoveryRate = totalExecCases > 0
                ? BigDecimal.valueOf(totalDefects).divide(BigDecimal.valueOf(totalExecCases), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;
        double executableRate = totalExecCount > 0
                ? BigDecimal.valueOf(successWithoutBlock).divide(BigDecimal.valueOf(totalExecCount), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

        dto.setVerificationDiscoveryRate(Math.round(defectDiscoveryRate * 100.0) / 100.0);
        dto.setExecutabilityRate(Math.round(executableRate * 100.0) / 100.0);

        double uqs = 0.4 * defectDiscoveryRate + 0.3 * executableRate + 0.3 * reuseRate;
        dto.setAvgUQSScore(Math.round(uqs * 100.0) / 100.0);
        return dto;
    }

    private RequirementQualityReuseMetricsDTO mapReuseMetrics(RequirementQualityReuseMetricsVO vo) {
        RequirementQualityReuseMetricsDTO dto = new RequirementQualityReuseMetricsDTO();
        if (vo == null) {
            dto.setReuseRateByCount(0.0);
            dto.setReuseRateByWorkload(0.0);
            dto.setAbsoluteTimeSavingsHours(0.0);
            dto.setReusedCaseCount(0L);
            dto.setTotalCaseCount(0L);
            dto.setReusedCsTotal(BigDecimal.ZERO);
            dto.setTotalCsTotal(BigDecimal.ZERO);
            return dto;
        }
        long total = vo.getTotalCaseCount() != null ? vo.getTotalCaseCount() : 0L;
        long reused = vo.getReusedCaseCount() != null ? vo.getReusedCaseCount() : 0L;
        BigDecimal totalCs = vo.getTotalCsTotal() != null ? vo.getTotalCsTotal() : BigDecimal.ZERO;
        BigDecimal reusedCs = vo.getReusedCsTotal() != null ? vo.getReusedCsTotal() : BigDecimal.ZERO;
        long savedMs = vo.getAbsoluteTimeSavingsMs() != null ? vo.getAbsoluteTimeSavingsMs() : 0L;

        dto.setTotalCaseCount(total);
        dto.setReusedCaseCount(reused);
        dto.setTotalCsTotal(totalCs);
        dto.setReusedCsTotal(reusedCs);
        dto.setReuseRateByCount(total > 0 ? Math.round(100.0 * reused / total * 100.0) / 100.0 : 0.0);
        dto.setReuseRateByWorkload(totalCs.compareTo(BigDecimal.ZERO) > 0
                ? BigDecimal.valueOf(100).multiply(reusedCs).divide(totalCs, 2, RoundingMode.HALF_UP).doubleValue()
                : 0.0);
        dto.setAbsoluteTimeSavingsHours(Math.round(savedMs / (1000.0 * 3600.0) * 100.0) / 100.0);
        return dto;
    }

    private RequirementQualityWorkHourByLevelVO mapWorkHourByLevel(
            List<Map<String, Object>> levelStats, Long actualWriteMinutes, Long actualExecMs) {
        RequirementQualityWorkHourByLevelVO vo = new RequirementQualityWorkHourByLevelVO();
        vo.setExpectedWriteMinutesL1(BigDecimal.ZERO);
        vo.setExpectedWriteMinutesL2(BigDecimal.ZERO);
        vo.setExpectedWriteMinutesL3(BigDecimal.ZERO);
        vo.setExpectedWriteMinutesL4(BigDecimal.ZERO);
        vo.setExpectedExecMinutesL1(BigDecimal.ZERO);
        vo.setExpectedExecMinutesL2(BigDecimal.ZERO);
        vo.setExpectedExecMinutesL3(BigDecimal.ZERO);
        vo.setExpectedExecMinutesL4(BigDecimal.ZERO);
        vo.setActualWriteMinutesTotal(actualWriteMinutes != null ? BigDecimal.valueOf(actualWriteMinutes) : BigDecimal.ZERO);
        vo.setActualExecMinutesTotal(actualExecMs != null ? BigDecimal.valueOf(actualExecMs / 60_000L) : BigDecimal.ZERO);

        if (levelStats != null && !levelStats.isEmpty()) {
            for (Map<String, Object> stat : levelStats) {
                String level = stat.get("complexity_level") != null ? stat.get("complexity_level").toString() : null;
                Long expectedWriteMs = toLong(stat.get("expected_write_ms"));
                Long expectedExecMs = toLong(stat.get("expected_exec_ms"));
                if (level == null) continue;
                BigDecimal writeMin = expectedWriteMs != null ? BigDecimal.valueOf(expectedWriteMs / 60_000L) : BigDecimal.ZERO;
                BigDecimal execMin = expectedExecMs != null ? BigDecimal.valueOf(expectedExecMs / 60_000L) : BigDecimal.ZERO;
                switch (level) {
                    case "L1":
                        vo.setExpectedWriteMinutesL1(writeMin);
                        vo.setExpectedExecMinutesL1(execMin);
                        break;
                    case "L2":
                        vo.setExpectedWriteMinutesL2(writeMin);
                        vo.setExpectedExecMinutesL2(execMin);
                        break;
                    case "L3":
                        vo.setExpectedWriteMinutesL3(writeMin);
                        vo.setExpectedExecMinutesL3(execMin);
                        break;
                    case "L4":
                        vo.setExpectedWriteMinutesL4(writeMin);
                        vo.setExpectedExecMinutesL4(execMin);
                        break;
                    default:
                        break;
                }
            }
        }
        return vo;
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Long) return (Long) o;
        if (o instanceof Number) return ((Number) o).longValue();
        try {
            return Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static final java.util.Map<String, String> PRIORITY_NAME_MAP = java.util.Map.of(
            "P0", "P0-核心流程",
            "P1", "P1-重要功能",
            "P2", "P2-一般功能",
            "P3", "P3-次要功能"
    );

    private List<RequirementQualityExecutionTrendDTO> mapExecutionTrend(List<RequirementQualityExecutionTrendVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return Collections.emptyList();
        }
        List<RequirementQualityExecutionTrendDTO> list = new ArrayList<>();
        for (RequirementQualityExecutionTrendVO vo : vos) {
            RequirementQualityExecutionTrendDTO dto = new RequirementQualityExecutionTrendDTO();
            String execDate = vo.getExecDate();
            dto.setDate(execDate != null && execDate.length() >= 10 ? execDate.substring(5, 10) : execDate);
            long p = vo.getPassed() != null ? vo.getPassed() : 0L;
            long f = vo.getFailed() != null ? vo.getFailed() : 0L;
            long b = vo.getBlocked() != null ? vo.getBlocked() : 0L;
            dto.setPassed(p);
            dto.setFailed(f);
            dto.setBlocked(b);
            long total = p + f + b;
            dto.setPassRate(total > 0 ? Math.round(100.0 * p / total * 100.0) / 100.0 : 0.0);
            list.add(dto);
        }
        return list;
    }

    private static final java.util.Map<String, String> BLOCK_REASON_NAME_MAP = java.util.Map.of(
            "ENVIRONMENT", "环境因素",
            "RESOURCE_SHORTAGE", "资源不足",
            "PREREQUISITE_DEPENDENCY", "前置依赖",
            "REQUIREMENT_UNCLEAR", "需求不明确",
            "TECHNICAL_DIFFICULTY", "技术难点",
            "PROCESS_COMMUNICATION", "流程沟通"
    );

    private static final java.util.Map<String, String> CHANGE_REASON_NAME_MAP = java.util.Map.of(
            "REQUIREMENT_TEMP", "需求临时变更",
            "REQUIREMENT_ITERATION", "需求迭代变更",
            "CASE_DESIGN", "用例设计变更",
            "CASE_MAINTENANCE", "历史用例维护",
            "TECH_SOLUTION", "技术方案适配",
            "RESOURCE_ADJUSTMENT", "资源配置调整",
            "EXTERNAL_DEPENDENCY", "外部依赖变更",
            "COMPLIANCE_POLICY", "合规政策要求",
            "SYS_DESIGN_CHANGE", "系分变更",
            "CASE_COPY", "copy变更"
    );

    private List<RequirementQualityReasonDistributionDTO> mapReasonDistribution(
            List<RequirementQualityReasonDistributionVO> vos,
            java.util.Map<String, String> nameMap) {
        if (vos == null || vos.isEmpty()) {
            return Collections.emptyList();
        }
        List<RequirementQualityReasonDistributionDTO> list = new ArrayList<>();
        for (RequirementQualityReasonDistributionVO vo : vos) {
            RequirementQualityReasonDistributionDTO dto = new RequirementQualityReasonDistributionDTO();
            String code = vo.getReasonCode();
            dto.setName(StringUtils.isNotBlank(code) ? nameMap.getOrDefault(code, code) : "其他");
            dto.setValue(vo.getCount() != null ? vo.getCount() : 0L);
            list.add(dto);
        }
        return list;
    }

    private static final java.util.Set<String> SUCCESS_RESULTS = java.util.Set.of("SUCCESS", "PASS");

    private List<RequirementQualityCaseExecutionRowDTO> mapCaseExecutionRows(List<RequirementQualityCaseExecutionRowVO> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<RequirementQualityCaseExecutionRowDTO> list = new ArrayList<>();
        for (RequirementQualityCaseExecutionRowVO vo : rows) {
            RequirementQualityCaseExecutionRowDTO dto = new RequirementQualityCaseExecutionRowDTO();
            dto.setPlanId(vo.getPlanId());
            dto.setCaseId(vo.getCaseId());
            dto.setName(StringUtils.isNotBlank(vo.getCaseName()) ? vo.getCaseName() : "未命名用例");
            long execCountSum = vo.getExecCountSum() != null ? vo.getExecCountSum() : 0L;
            long totalTimeMs = vo.getTotalTimeMs() != null ? vo.getTotalTimeMs() : 0L;
            Long successFromRecord = vo.getSuccessCountRecord() != null ? vo.getSuccessCountRecord() : 0L;
            Long failFromRecord = vo.getFailCountRecord() != null ? vo.getFailCountRecord() : 0L;
            long recordTotal = successFromRecord + failFromRecord;
            String lastResult = StringUtils.isNotBlank(vo.getLastExecResultMetrics())
                    ? vo.getLastExecResultMetrics()
                    : vo.getLastExecResultTpfc();
            boolean isSuccess = lastResult != null && SUCCESS_RESULTS.contains(lastResult.toUpperCase());
            int execCount;
            int successCount;
            int failCount;
            long rateBaseCount; // 用于计算成功率/失败率的分母，保证失败率+成功率=100%
            if (recordTotal > 0) {
                execCount = (int) Math.max(execCountSum, recordTotal);
                successCount = successFromRecord.intValue();
                failCount = failFromRecord.intValue();
                rateBaseCount = recordTotal; // 费率分母仅包含明确成功/失败的执行
            } else {
                execCount = (int) (execCountSum > 0 ? execCountSum : (StringUtils.isNotBlank(lastResult) ? 1 : 0));
                successCount = isSuccess ? execCount : 0;
                failCount = execCount - successCount;
                rateBaseCount = execCount;
            }
            dto.setExecCount(execCount);
            dto.setSuccessCount(successCount);
            dto.setFailCount(failCount);
            // 平均耗时：优先用 case_execution_record（只统计 exec_duration > 0）；无数据时兜底用 test_plan_case_metrics
            long totalDurationMsFromCer = vo.getTotalDurationMsFromCer() != null ? vo.getTotalDurationMsFromCer() : 0L;
            long durationPositiveCount = vo.getDurationPositiveCount() != null ? vo.getDurationPositiveCount() : 0L;
            double avgSeconds;
            if (durationPositiveCount > 0) {
                avgSeconds = (totalDurationMsFromCer / 1000.0) / durationPositiveCount;
            } else {
                // 兜底：case_execution_record 无有效数据时，用 test_plan_case_metrics 的 total_time_ms / exec_count
                avgSeconds = execCount > 0 ? (totalTimeMs / 1000.0) / execCount : 0;
            }
            dto.setAvgTimeSeconds(Math.round(avgSeconds * 100.0) / 100.0);
            // 最大耗时：优先用 case_execution_record；无数据时兜底与平均一致（老逻辑）
            Long maxMs = vo.getMaxDurationMs();
            if (maxMs != null && maxMs > 0) {
                dto.setMaxTimeSeconds(Math.round((maxMs / 1000.0) * 100.0) / 100.0);
            } else {
                dto.setMaxTimeSeconds(dto.getAvgTimeSeconds());
            }
            double failRate = rateBaseCount > 0 ? 100.0 * failCount / rateBaseCount : 0;
            double successRate = rateBaseCount > 0 ? 100.0 * successCount / rateBaseCount : 0;
            dto.setFailRate(Math.round(failRate * 100.0) / 100.0);
            dto.setSuccessRate(Math.round(successRate * 100.0) / 100.0);
            dto.setFailReason(execCount > 0 && !isSuccess ? mapFailReason(lastResult) : null);
            list.add(dto);
        }
        return list;
    }

    private String mapFailReason(String lastResult) {
        if (lastResult == null) return null;
        switch (lastResult.toUpperCase()) {
            case "ERROR":
                return "失败";
            case "BLOCKED":
                return "阻塞";
            case "FAKE_ERROR":
                return "误报";
            default:
                return lastResult;
        }
    }

    /**
     * 筛选项：项目列表（有关联测试计划组的）、需求列表（已关联测试计划组的）、状态（未开始/进行中/已完成）
     */
    public RequirementQualityFilterOptionsDTO getFilterOptions() {
        RequirementQualityFilterOptionsDTO dto = new RequirementQualityFilterOptionsDTO();
        List<String> projectIds = extRequirementQualityMapper.selectProjectIdsWithLinkedGroup();
        if (projectIds != null && !projectIds.isEmpty()) {
            List<Project> projects = baseProjectMapper.selectProjectByIdList(projectIds);
            dto.setProjectOptions(projects != null ? projects.stream()
                    .map(p -> new OptionDTO(p.getId(), p.getName()))
                    .collect(Collectors.toList()) : Collections.emptyList());
        } else {
            dto.setProjectOptions(Collections.emptyList());
        }
        List<OptionDTO> reqOptions = extRequirementQualityMapper.selectRequirementOptionsWithLinkedGroup();
        dto.setRequirementOptions(reqOptions != null ? reqOptions : Collections.emptyList());
        dto.setStatusOptions(List.of(
                new OptionDTO("PREPARED", "未开始"),
                new OptionDTO("UNDERWAY", "进行中"),
                new OptionDTO("COMPLETED", "已完成"),
                new OptionDTO("ARCHIVED", "已归档")));
        return dto;
    }

    /**
     * 需求库关键词搜索：从完整需求库 meego_story_stats 按 story_id / story_name 模糊匹配，供门禁补全弹窗选择需求
     */
    public List<OptionDTO> searchStoryByKeyword(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return Collections.emptyList();
        }
        List<OptionDTO> list = extRequirementQualityMapper.selectStorySearchByKeyword(keyword.trim());
        return list != null ? list : Collections.emptyList();
    }

    /**
     * 根据需求 ID 列表批量查需求名称（供缺陷列表等展示需求列名称）
     */
    public List<OptionDTO> getStoryNamesByIds(List<String> storyIds) {
        if (storyIds == null || storyIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> distinctIds = storyIds.stream().filter(StringUtils::isNotBlank).distinct().toList();
        if (distinctIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<OptionDTO> list = extRequirementQualityMapper.selectStoryNamesByIds(distinctIds);
        return list != null ? list : Collections.emptyList();
    }
}
