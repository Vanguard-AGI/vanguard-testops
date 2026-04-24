package io.vanguard.testops.functional.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.vanguard.testops.functional.dto.dashboard.EfficiencyOverviewRequest;
import io.vanguard.testops.functional.dto.dashboard.EfficiencyOverviewResponse;
import io.vanguard.testops.functional.dto.dashboard.UserActivityResponse;
import io.vanguard.testops.functional.mapper.ExtFunctionalCaseMapper;
import io.vanguard.testops.functional.mapper.MockSceneRuleMapper;
import io.vanguard.testops.functional.mapper.TestPlanFunctionalCaseActivityMapper;
import io.vanguard.testops.functional.mapper.TestPlanStatsMapper;
import io.vanguard.testops.metadata.domain.MetadataDefinition;
import io.vanguard.testops.metadata.domain.MetadataOperationLog;
import io.vanguard.testops.metadata.domain.ScriptManage;
import io.vanguard.testops.metadata.mapper.MetadataDefinitionMapper;
import io.vanguard.testops.metadata.mapper.MetadataOperationLogMapper;
import io.vanguard.testops.metadata.mapper.ScriptManageMapper;
import io.vanguard.testops.system.domain.User;
import io.vanguard.testops.system.domain.UserExample;
import io.vanguard.testops.system.dto.OnlineUserStats;
import io.vanguard.testops.system.mapper.BaseUserMapper;
import io.vanguard.testops.system.mapper.UserMapper;
import io.vanguard.testops.system.service.OnlineUserService;
import io.vanguard.testops.workflow.domain.WorkflowDefinition;
import io.vanguard.testops.workflow.domain.WorkflowRun;
import io.vanguard.testops.workflow.mapper.WorkflowDefinitionMapper;
import io.vanguard.testops.workflow.mapper.WorkflowRunMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 元数据指标统计服务
 * 用于处理从 runner_operation_log 迁移到 metadata_operation_log 的指标计算
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MetadataStatisticsService {

    @Resource
    private MetadataOperationLogMapper metadataOperationLogMapper;

    @Resource
    private MetadataDefinitionMapper metadataDefinitionMapper;

    @Resource
    private MockSceneRuleMapper mockSceneRuleMapper;
    
    @Resource
    private ScriptManageMapper scriptManageMapper;

    @Resource
    private WorkflowDefinitionMapper workflowDefinitionMapper;

    @Resource
    private WorkflowRunMapper workflowRunMapper;

    @Resource
    private TestPlanStatsMapper testPlanStatsMapper;

    @Resource
    private TestPlanFunctionalCaseActivityMapper testPlanFunctionalCaseActivityMapper;

    @Resource
    private ExtFunctionalCaseMapper extFunctionalCaseMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private BaseUserMapper baseUserMapper;

    @Resource
    private OnlineUserService onlineUserService;

    /**
     * 获取 SnapTest 概览数据
     */
    public EfficiencyOverviewResponse getEfficiencyOverview(EfficiencyOverviewRequest request) {
        // 计算造数提效总时长（小时）, 造数提效率
        EfficiencyOverviewResponse.DataGenerationEfficiency dataGenerationResult =
                computeDataGenerationEfficiency(request);

        // 造数工厂：总数（metadata_definition protocol=SCRIPT）+ 执行数量（metadata_operation_log）
        Long dataFactoryTotal = computeDataFactoryTotal(request);
        Long dataFactoryExecutionCount = computeDataFactoryExecutionCount(request);
        EfficiencyOverviewResponse.DataFactoryStats dataFactoryStats = new EfficiencyOverviewResponse.DataFactoryStats();
        dataFactoryStats.setTotal(dataFactoryTotal != null ? dataFactoryTotal : 0L);
        dataFactoryStats.setExecutionCount(dataFactoryExecutionCount != null ? dataFactoryExecutionCount : 0L);

        // 自动化运行数据：spotter_aegis.workflow_run，trigger_user/create_time，deleted_time IS NULL，总数+成功率
        EfficiencyOverviewResponse.AutomationRunStats automationRunStats = computeAutomationRunStats(request);

        // 功能用例执行：spotter_aegis.test_plan_functional_case 关联 test_plan，projectIds/userId/last_exec_time，总数+成功率（last_exec_result=SUCCESS）
        EfficiencyOverviewResponse.FunctionalCaseExecutionStats functionalCaseExecutionStats = computeFunctionalCaseExecutionStats(request);

        // MQ：总数（metadata_definition protocol=ROCKETMQ）+ 使用次数（metadata_operation_log）
        Long mqTotal = computeMqTotal(request);
        Integer mqUsageCount = computeMqUsageCount(request);
        EfficiencyOverviewResponse.MqStats mqStats = new EfficiencyOverviewResponse.MqStats();
        mqStats.setTotal(mqTotal != null ? mqTotal : 0L);
        mqStats.setUsageCount(mqUsageCount != null ? mqUsageCount.longValue() : 0L);

        // Mock工厂：总数（spotter_runner.mock_scene_rule deleted_at IS NULL，按时间查询）+ 执行总数（metadata_operation_log module_type=MOCK, action=execute）
        Long mockFactoryTotal = computeMockFactoryTotal(request);
        Long mockFactoryExecutionCount = computeMockExecutionCount(request);
        EfficiencyOverviewResponse.MockFactoryStats mockFactoryStats = new EfficiencyOverviewResponse.MockFactoryStats();
        mockFactoryStats.setTotal(mockFactoryTotal != null ? mockFactoryTotal : 0L);
        mockFactoryStats.setExecutionCount(mockFactoryExecutionCount != null ? mockFactoryExecutionCount : 0L);

        // 自动化总数：workflow_definition 表，deleted_time IS NULL，支持 projectIds、userId 筛选（同 MQ 总数）
        Long automationTotal = computeAutomationTotalWithFilter(request);
        EfficiencyOverviewResponse.AutomationStats automationStats = new EfficiencyOverviewResponse.AutomationStats();
        automationStats.setTotal(automationTotal != null ? automationTotal : 0L);

        // 测试计划总数：spotter_aegis.test_plan，按 group_id 去重且排除 NONE，支持 projectIds、userId 筛选（同 MQ 总数）
        Long testPlanTotal = computeTestPlanTotalWithFilter(request);
        EfficiencyOverviewResponse.TestPlanStats testPlanStats = new EfficiencyOverviewResponse.TestPlanStats();
        testPlanStats.setTotal(testPlanTotal != null ? testPlanTotal : 0L);


        EfficiencyOverviewResponse response = new EfficiencyOverviewResponse();
        response.setDataGenerationEfficiency(dataGenerationResult);
        response.setDataFactoryStats(dataFactoryStats);
        response.setMqStats(mqStats);
        response.setMockFactoryStats(mockFactoryStats);
        response.setAutomationStats(automationStats);
        response.setTestPlanStats(testPlanStats);
        response.setAutomationRunStats(automationRunStats);
        response.setFunctionalCaseExecutionStats(functionalCaseExecutionStats);

        // AI 用例生成指标：时间范围内新增的 AI/手工用例及占比、AI 新增率（需 startDate/endDate）
        EfficiencyOverviewResponse.AiCaseStats aiCaseStats = computeAiCaseStats(request);
        response.setAiCaseStats(aiCaseStats);

        // 同比/环比：对比周期统计（仅当 comparisonType 有效且提供了 startDate/endDate 时计算），并回填对比维度信息
        applyComparisonStatsIfNeeded(request, response, dataFactoryStats, mqStats, mockFactoryStats,
                automationStats, testPlanStats, automationRunStats, functionalCaseExecutionStats, aiCaseStats);

        return response;
    }

    /**
     * 造数工厂总数：metadata_definition 表 protocol=SCRIPT，deleted_time IS NULL，支持 projectIds、userId 筛选
     */
    private Long computeDataFactoryTotal(EfficiencyOverviewRequest request) {
        LambdaQueryWrapper<MetadataDefinition> wrapper = buildMetadataDefinitionQueryWrapperForTotal(request, "SCRIPT");
        Long count = metadataDefinitionMapper.selectCount(wrapper);
        return count != null ? count : 0L;
    }

    /**
     * 造数工厂执行数量（bizType=1, module_type=SCRIPT, action=execute）
     */
    private Long computeDataFactoryExecutionCount(EfficiencyOverviewRequest request) {
        LambdaQueryWrapper<MetadataOperationLog> queryWrapper = buildBaseQueryWrapper(request);
        queryWrapper.eq(MetadataOperationLog::getBizType, 1)
                   .eq(MetadataOperationLog::getModuleType, "SCRIPT")
                   .eq(MetadataOperationLog::getAction, "execute");
        Long count = metadataOperationLogMapper.selectCount(queryWrapper);
        return count != null ? count : 0L;
    }

    /**
     * MQ 总数：metadata_definition 表 protocol=ROCKETMQ，deleted_time IS NULL，支持 projectIds、userId 筛选
     */
    private Long computeMqTotal(EfficiencyOverviewRequest request) {
        LambdaQueryWrapper<MetadataDefinition> wrapper = buildMetadataDefinitionQueryWrapperForTotal(request, "ROCKETMQ");
        Long count = metadataDefinitionMapper.selectCount(wrapper);
        return count != null ? count : 0L;
    }

    /**
     * Mock工厂总数（基数）：spotter_runner.mock_scene_rule 中 deleted_at IS NULL 的数量，不受查询条件影响
     */
    private Long computeMockFactoryTotal(EfficiencyOverviewRequest request) {
        // 基数总数：不带任何时间/人员过滤
        return mockSceneRuleMapper.countByDeletedAtIsNull(null, null);
    }

    /**
     * Mock工厂执行总数：metadata_operation_log module_type=MOCK, action=execute
     */
    private Long computeMockExecutionCount(EfficiencyOverviewRequest request) {
        LambdaQueryWrapper<MetadataOperationLog> queryWrapper = buildBaseQueryWrapper(request);
        queryWrapper.eq(MetadataOperationLog::getModuleType, "MOCK")
                   .eq(MetadataOperationLog::getAction, "execute");
        Long count = metadataOperationLogMapper.selectCount(queryWrapper);
        return count != null ? count : 0L;
    }

    /**
     * 自动化总数（仅 projectIds、userId 筛选，无日期）：用于概览总数及同环比 current，与 MQ 总数口径一致
     */
    private Long computeAutomationTotalWithFilter(EfficiencyOverviewRequest request) {
        LambdaQueryWrapper<WorkflowDefinition> wrapper = buildWorkflowDefinitionQueryWrapperForTotal(request);
        Long count = workflowDefinitionMapper.selectCount(wrapper);
        return count != null ? count : 0L;
    }

    /**
     * 构建 workflow_definition 总数查询条件（仅 projectIds、userId，无日期）
     */
    private LambdaQueryWrapper<WorkflowDefinition> buildWorkflowDefinitionQueryWrapperForTotal(
            EfficiencyOverviewRequest request) {
        LambdaQueryWrapper<WorkflowDefinition> wrapper = Wrappers.lambdaQuery();
        wrapper.isNull(WorkflowDefinition::getDeletedTime);
        if (request.getProjectIds() != null && !request.getProjectIds().isEmpty()) {
            wrapper.in(WorkflowDefinition::getProjectId, request.getProjectIds());
        }
        if (StringUtils.isNotBlank(request.getUserId())) {
            wrapper.eq(WorkflowDefinition::getCreateUser, request.getUserId());
        }
        return wrapper;
    }

    /**
     * 自动化总数（带筛选，含日期）：用于同比/环比对比期统计
     */
    private Long computeAutomationTotal(EfficiencyOverviewRequest request) {
        LambdaQueryWrapper<WorkflowDefinition> wrapper = buildWorkflowDefinitionQueryWrapper(request);
        Long count = workflowDefinitionMapper.selectCount(wrapper);
        return count != null ? count : 0L;
    }

    /**
     * 构建 workflow_definition 查询条件：deleted_time IS NULL、projectIds、userId(create_user)、可选 create_time 时间范围
     * 与 metadata_definition 查询逻辑一致
     */
    private LambdaQueryWrapper<WorkflowDefinition> buildWorkflowDefinitionQueryWrapper(
            EfficiencyOverviewRequest request) {
        LambdaQueryWrapper<WorkflowDefinition> wrapper = Wrappers.lambdaQuery();
        wrapper.isNull(WorkflowDefinition::getDeletedTime);
        if (request.getProjectIds() != null && !request.getProjectIds().isEmpty()) {
            wrapper.in(WorkflowDefinition::getProjectId, request.getProjectIds());
        }
        if (StringUtils.isNotBlank(request.getUserId())) {
            wrapper.eq(WorkflowDefinition::getCreateUser, request.getUserId());
        }
        if (StringUtils.isNotBlank(request.getStartDate()) && StringUtils.isNotBlank(request.getEndDate())) {
            LocalDate startDate = LocalDate.parse(request.getStartDate(), DateTimeFormatter.ISO_DATE);
            LocalDate endDate = LocalDate.parse(request.getEndDate(), DateTimeFormatter.ISO_DATE);
            long startTs = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endTs = endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            wrapper.ge(WorkflowDefinition::getCreateTime, startTs)
                   .le(WorkflowDefinition::getCreateTime, endTs);
        }
        return wrapper;
    }

    /**
     * 构建 workflow_definition 查询条件（截止某时刻）：create_time &lt;= endTs，用于 total 场景 previous
     */
    private LambdaQueryWrapper<WorkflowDefinition> buildWorkflowDefinitionQueryWrapperAsOf(
            EfficiencyOverviewRequest request, long endTs) {
        LambdaQueryWrapper<WorkflowDefinition> wrapper = Wrappers.lambdaQuery();
        wrapper.isNull(WorkflowDefinition::getDeletedTime)
               .le(WorkflowDefinition::getCreateTime, endTs);
        if (request.getProjectIds() != null && !request.getProjectIds().isEmpty()) {
            wrapper.in(WorkflowDefinition::getProjectId, request.getProjectIds());
        }
        if (StringUtils.isNotBlank(request.getUserId())) {
            wrapper.eq(WorkflowDefinition::getCreateUser, request.getUserId());
        }
        return wrapper;
    }

    /**
     * 测试计划总数（仅 projectIds、userId 筛选，无日期）：用于概览总数及同环比 current，与 MQ 总数口径一致
     */
    private Long computeTestPlanTotalWithFilter(EfficiencyOverviewRequest request) {
        List<String> projectIds = (request.getProjectIds() != null && !request.getProjectIds().isEmpty())
                ? request.getProjectIds() : null;
        String createUser = StringUtils.isNotBlank(request.getUserId()) ? request.getUserId() : null;
        return testPlanStatsMapper.countDistinctGroupIdExcludingNone(projectIds, createUser, null, null);
    }

    /**
     * 测试计划总数（带筛选，含日期）：用于同比/环比对比期统计
     */
    private Long computeTestPlanTotal(EfficiencyOverviewRequest request) {
        List<String> projectIds = (request.getProjectIds() != null && !request.getProjectIds().isEmpty())
                ? request.getProjectIds() : null;
        String createUser = StringUtils.isNotBlank(request.getUserId()) ? request.getUserId() : null;
        Long createTimeStart = null;
        Long createTimeEnd = null;
        if (StringUtils.isNotBlank(request.getStartDate()) && StringUtils.isNotBlank(request.getEndDate())) {
            LocalDate startDate = LocalDate.parse(request.getStartDate(), DateTimeFormatter.ISO_DATE);
            LocalDate endDate = LocalDate.parse(request.getEndDate(), DateTimeFormatter.ISO_DATE);
            createTimeStart = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            createTimeEnd = endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        long count = testPlanStatsMapper.countDistinctGroupIdExcludingNone(projectIds, createUser, createTimeStart, createTimeEnd);
        return count;
    }

    /**
     * 自动化运行数据：spotter_aegis.workflow_run，trigger_user/create_time，deleted_time IS NULL，总数+成功率（SUCCESS/总数×100%）
     */
    private EfficiencyOverviewResponse.AutomationRunStats computeAutomationRunStats(EfficiencyOverviewRequest request) {
        LambdaQueryWrapper<WorkflowRun> wrapper = buildWorkflowRunQueryWrapper(request);
        Long total = workflowRunMapper.selectCount(wrapper);
        long totalCount = total != null ? total : 0L;

        LambdaQueryWrapper<WorkflowRun> successWrapper = buildWorkflowRunQueryWrapper(request);
        successWrapper.eq(WorkflowRun::getStatus, "SUCCESS");
        Long successCountLong = workflowRunMapper.selectCount(successWrapper);
        long successCount = successCountLong != null ? successCountLong : 0L;

        EfficiencyOverviewResponse.AutomationRunStats stats = new EfficiencyOverviewResponse.AutomationRunStats();
        stats.setTotal(totalCount);
        if (totalCount > 0) {
            stats.setSuccessRate(Math.round(successCount * 10000.0 / totalCount) / 100.0);
        } else {
            stats.setSuccessRate(null);
        }
        return stats;
    }

    /**
     * 构建 workflow_run 查询条件：deleted_time IS NULL、projectIds、trigger_user、可选 create_time 时间范围
     * 与 metadata_definition 查询逻辑一致（用户对应 trigger_user，时间为 create_time 毫秒）
     */
    private LambdaQueryWrapper<WorkflowRun> buildWorkflowRunQueryWrapper(EfficiencyOverviewRequest request) {
        LambdaQueryWrapper<WorkflowRun> wrapper = Wrappers.lambdaQuery();
        wrapper.isNull(WorkflowRun::getDeletedTime);
        if (request.getProjectIds() != null && !request.getProjectIds().isEmpty()) {
            wrapper.in(WorkflowRun::getProjectId, request.getProjectIds());
        }
        if (StringUtils.isNotBlank(request.getUserId())) {
            wrapper.eq(WorkflowRun::getTriggerUser, request.getUserId());
        }
        if (StringUtils.isNotBlank(request.getStartDate()) && StringUtils.isNotBlank(request.getEndDate())) {
            LocalDate startDate = LocalDate.parse(request.getStartDate(), DateTimeFormatter.ISO_DATE);
            LocalDate endDate = LocalDate.parse(request.getEndDate(), DateTimeFormatter.ISO_DATE);
            long startTs = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endTs = endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            wrapper.ge(WorkflowRun::getCreateTime, startTs)
                   .le(WorkflowRun::getCreateTime, endTs);
        }
        return wrapper;
    }

    /**
     * 自动化运行总数（用于同比/环比）：workflow_run 在请求条件下的 count
     */
    private long computeAutomationRunTotal(EfficiencyOverviewRequest request) {
        Long count = workflowRunMapper.selectCount(buildWorkflowRunQueryWrapper(request));
        return count != null ? count : 0L;
    }

    /**
     * 自动化运行成功数（status=SUCCESS，用于同比/环比）
     */
    private long computeAutomationRunSuccessCount(EfficiencyOverviewRequest request) {
        LambdaQueryWrapper<WorkflowRun> wrapper = buildWorkflowRunQueryWrapper(request);
        wrapper.eq(WorkflowRun::getStatus, "SUCCESS");
        Long count = workflowRunMapper.selectCount(wrapper);
        return count != null ? count : 0L;
    }

    /**
     * AI 用例生成指标：functional_case 表，时间范围内新增的 AI/手工用例及占比、AI 新增率
     * 需 startDate/endDate，否则返回 null
     */
    private EfficiencyOverviewResponse.AiCaseStats computeAiCaseStats(EfficiencyOverviewRequest request) {
        if (StringUtils.isBlank(request.getStartDate()) || StringUtils.isBlank(request.getEndDate())) {
            return null;
        }
        List<String> projectIds = (request.getProjectIds() != null && !request.getProjectIds().isEmpty())
                ? request.getProjectIds() : null;
        String createUser = StringUtils.isNotBlank(request.getUserId()) ? request.getUserId() : null;
        LocalDate startDate = LocalDate.parse(request.getStartDate(), DateTimeFormatter.ISO_DATE);
        LocalDate endDate = LocalDate.parse(request.getEndDate(), DateTimeFormatter.ISO_DATE);
        Long createTimeStart = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        Long createTimeEnd = endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        long aiCaseCount = extFunctionalCaseMapper.countAiCaseInRange(projectIds, createUser, createTimeStart, createTimeEnd);
        long manualCaseCount = extFunctionalCaseMapper.countManualCaseInRange(projectIds, createUser, createTimeStart, createTimeEnd);
        long totalNewCount = aiCaseCount + manualCaseCount;

        EfficiencyOverviewResponse.AiCaseStats stats = new EfficiencyOverviewResponse.AiCaseStats();
        stats.setAiCaseCount(aiCaseCount);
        stats.setManualCaseCount(manualCaseCount);
        if (totalNewCount > 0) {
            double aiRatio = Math.round(aiCaseCount * 10000.0 / totalNewCount) / 100.0;
            double manualRatio = Math.round(manualCaseCount * 10000.0 / totalNewCount) / 100.0;
            stats.setAiRatio(aiRatio);
            stats.setManualRatio(manualRatio);
            stats.setAiCaseNewRate(aiRatio);
        } else {
            stats.setAiRatio(null);
            stats.setManualRatio(null);
            stats.setAiCaseNewRate(null);
        }
        return stats;
    }

    /**
     * AI 用例数（用于同比/环比）：时间范围内新增的 ai_create=true 用例数
     */
    private long computeAiCaseCountForRequest(EfficiencyOverviewRequest request) {
        if (StringUtils.isBlank(request.getStartDate()) || StringUtils.isBlank(request.getEndDate())) {
            return 0L;
        }
        List<String> projectIds = (request.getProjectIds() != null && !request.getProjectIds().isEmpty())
                ? request.getProjectIds() : null;
        String createUser = StringUtils.isNotBlank(request.getUserId()) ? request.getUserId() : null;
        LocalDate startDate = LocalDate.parse(request.getStartDate(), DateTimeFormatter.ISO_DATE);
        LocalDate endDate = LocalDate.parse(request.getEndDate(), DateTimeFormatter.ISO_DATE);
        Long createTimeStart = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        Long createTimeEnd = endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return extFunctionalCaseMapper.countAiCaseInRange(projectIds, createUser, createTimeStart, createTimeEnd);
    }

    /**
     * 手工用例数（用于同比/环比）：时间范围内新增的 ai_create=false 用例数
     */
    private long computeManualCaseCountForRequest(EfficiencyOverviewRequest request) {
        if (StringUtils.isBlank(request.getStartDate()) || StringUtils.isBlank(request.getEndDate())) {
            return 0L;
        }
        List<String> projectIds = (request.getProjectIds() != null && !request.getProjectIds().isEmpty())
                ? request.getProjectIds() : null;
        String createUser = StringUtils.isNotBlank(request.getUserId()) ? request.getUserId() : null;
        LocalDate startDate = LocalDate.parse(request.getStartDate(), DateTimeFormatter.ISO_DATE);
        LocalDate endDate = LocalDate.parse(request.getEndDate(), DateTimeFormatter.ISO_DATE);
        Long createTimeStart = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        Long createTimeEnd = endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return extFunctionalCaseMapper.countManualCaseInRange(projectIds, createUser, createTimeStart, createTimeEnd);
    }

    /**
     * 功能用例执行数据：test_plan_functional_case 关联 test_plan，支持 projectIds、userId(execute_user)、时间(last_exec_time)
     * 总数=有执行记录且在时间范围内的条数，成功率=last_exec_result=SUCCESS 数/总数×100%
     */
    private EfficiencyOverviewResponse.FunctionalCaseExecutionStats computeFunctionalCaseExecutionStats(EfficiencyOverviewRequest request) {
        List<String> projectIds = (request.getProjectIds() != null && !request.getProjectIds().isEmpty())
                ? request.getProjectIds() : null;
        String executeUser = StringUtils.isNotBlank(request.getUserId()) ? request.getUserId() : null;
        Long lastExecTimeStart = null;
        Long lastExecTimeEnd = null;
        if (StringUtils.isNotBlank(request.getStartDate()) && StringUtils.isNotBlank(request.getEndDate())) {
            LocalDate startDate = LocalDate.parse(request.getStartDate(), DateTimeFormatter.ISO_DATE);
            LocalDate endDate = LocalDate.parse(request.getEndDate(), DateTimeFormatter.ISO_DATE);
            lastExecTimeStart = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            lastExecTimeEnd = endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        long totalCount = testPlanFunctionalCaseActivityMapper.countExecutionInRange(projectIds, executeUser, lastExecTimeStart, lastExecTimeEnd);
        long successCount = testPlanFunctionalCaseActivityMapper.countSuccessInRange(projectIds, executeUser, lastExecTimeStart, lastExecTimeEnd);
        EfficiencyOverviewResponse.FunctionalCaseExecutionStats stats = new EfficiencyOverviewResponse.FunctionalCaseExecutionStats();
        stats.setTotal(totalCount);
        if (totalCount > 0) {
            stats.setSuccessRate(Math.round(successCount * 10000.0 / totalCount) / 100.0);
        } else {
            stats.setSuccessRate(null);
        }
        return stats;
    }

    /**
     * 功能用例执行总数（用于同比/环比）：test_plan_functional_case 在请求条件下的 count
     */
    private long computeFunctionalCaseExecutionTotal(EfficiencyOverviewRequest request) {
        List<String> projectIds = (request.getProjectIds() != null && !request.getProjectIds().isEmpty())
                ? request.getProjectIds() : null;
        String executeUser = StringUtils.isNotBlank(request.getUserId()) ? request.getUserId() : null;
        Long lastExecTimeStart = null;
        Long lastExecTimeEnd = null;
        if (StringUtils.isNotBlank(request.getStartDate()) && StringUtils.isNotBlank(request.getEndDate())) {
            LocalDate startDate = LocalDate.parse(request.getStartDate(), DateTimeFormatter.ISO_DATE);
            LocalDate endDate = LocalDate.parse(request.getEndDate(), DateTimeFormatter.ISO_DATE);
            lastExecTimeStart = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            lastExecTimeEnd = endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        return testPlanFunctionalCaseActivityMapper.countExecutionInRange(projectIds, executeUser, lastExecTimeStart, lastExecTimeEnd);
    }

    /**
     * 功能用例执行成功数（last_exec_result=SUCCESS，用于同比/环比）
     */
    private long computeFunctionalCaseExecutionSuccessCount(EfficiencyOverviewRequest request) {
        List<String> projectIds = (request.getProjectIds() != null && !request.getProjectIds().isEmpty())
                ? request.getProjectIds() : null;
        String executeUser = StringUtils.isNotBlank(request.getUserId()) ? request.getUserId() : null;
        Long lastExecTimeStart = null;
        Long lastExecTimeEnd = null;
        if (StringUtils.isNotBlank(request.getStartDate()) && StringUtils.isNotBlank(request.getEndDate())) {
            LocalDate startDate = LocalDate.parse(request.getStartDate(), DateTimeFormatter.ISO_DATE);
            LocalDate endDate = LocalDate.parse(request.getEndDate(), DateTimeFormatter.ISO_DATE);
            lastExecTimeStart = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            lastExecTimeEnd = endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        return testPlanFunctionalCaseActivityMapper.countSuccessInRange(projectIds, executeUser, lastExecTimeStart, lastExecTimeEnd);
    }

    /**
     * 应用同比/环比对比统计：为六类指标填充 comparison 嵌套结构，并设置响应的对比维度信息。
     * 同比(YOY)：与上一年度同一时期对比；环比(MOM)：与紧邻上一同统计周期对比（如最近7天 vs 前7天）。
     */
    private void applyComparisonStatsIfNeeded(EfficiencyOverviewRequest request,
                                              EfficiencyOverviewResponse response,
                                              EfficiencyOverviewResponse.DataFactoryStats dataFactoryStats,
                                              EfficiencyOverviewResponse.MqStats mqStats,
                                              EfficiencyOverviewResponse.MockFactoryStats mockFactoryStats,
                                              EfficiencyOverviewResponse.AutomationStats automationStats,
                                              EfficiencyOverviewResponse.TestPlanStats testPlanStats,
                                              EfficiencyOverviewResponse.AutomationRunStats automationRunStats,
                                              EfficiencyOverviewResponse.FunctionalCaseExecutionStats functionalCaseExecutionStats,
                                              EfficiencyOverviewResponse.AiCaseStats aiCaseStats) {
        if (StringUtils.isBlank(request.getComparisonType())
                || StringUtils.isBlank(request.getStartDate())
                || StringUtils.isBlank(request.getEndDate())) {
            return;
        }

        String type = request.getComparisonType().toUpperCase(Locale.ROOT);
        if (!"YOY".equals(type) && !"MOM".equals(type)) {
            return;
        }

        LocalDate start = LocalDate.parse(request.getStartDate(), DateTimeFormatter.ISO_DATE);
        LocalDate end = LocalDate.parse(request.getEndDate(), DateTimeFormatter.ISO_DATE);
        long days = ChronoUnit.DAYS.between(start, end) + 1;

        LocalDate compareStart;
        LocalDate compareEnd;
        if ("YOY".equals(type)) {
            compareStart = start.minusYears(1);
            compareEnd = end.minusYears(1);
        } else {
            compareStart = start.minusDays(days);
            compareEnd = end.minusDays(days);
        }

        response.setComparisonType(type);
        response.setComparisonPeriod(compareStart.format(DateTimeFormatter.ISO_DATE) + "~" + compareEnd.format(DateTimeFormatter.ISO_DATE));
        response.setComparisonLabel("YOY".equals(type) ? "同比（与上一年同期对比）" : "环比（与上一统计周期对比）");

        EfficiencyOverviewRequest compareRequest = new EfficiencyOverviewRequest();
        compareRequest.setStartDate(compareStart.format(DateTimeFormatter.ISO_DATE));
        compareRequest.setEndDate(compareEnd.format(DateTimeFormatter.ISO_DATE));
        compareRequest.setPersonal(request.getPersonal());
        compareRequest.setProjectIds(request.getProjectIds());
        compareRequest.setUserId(request.getUserId());
        compareRequest.setComparisonType(request.getComparisonType());

        // 截止上期结束时间（用于 total 场景：previous = 截止上期截止时间的总数）
        long compareEndTs = compareEnd.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        LocalDateTime compareEndDateTime = compareEnd.atTime(23, 59, 59);

        // 造数工厂：同比/环比。total 场景 current=顶层总数，previous=截止上期结束的总数；executionCount 为周期内数量
        long dfTotalCurrent = dataFactoryStats.getTotal() != null ? dataFactoryStats.getTotal() : 0L;
        long dfTotalPrevious = computeDataFactoryTotalAsOf(compareEndTs, request);
        Long compareDfExec = computeDataFactoryExecutionCount(compareRequest);
        long currentDfExec = dataFactoryStats.getExecutionCount() != null ? dataFactoryStats.getExecutionCount() : 0L;
        EfficiencyOverviewResponse.DataFactoryComparison dfComp = new EfficiencyOverviewResponse.DataFactoryComparison();
        dfComp.setTotal(buildComparisonItem(dfTotalPrevious, dfTotalCurrent));
        dfComp.setExecutionCount(buildComparisonItem(compareDfExec != null ? compareDfExec : 0L, currentDfExec));
        dataFactoryStats.setComparison(dfComp);

        // MQ：同比/环比。total 场景 current=顶层总数，previous=截止上期结束的总数
        long mqTotalCurrent = mqStats.getTotal() != null ? mqStats.getTotal() : 0L;
        long mqTotalPrevious = computeMqTotalAsOf(compareEndTs, request);
        Integer compareMqUsage = computeMqUsageCount(compareRequest);
        long currentMqUsage = mqStats.getUsageCount() != null ? mqStats.getUsageCount() : 0L;
        EfficiencyOverviewResponse.MqComparison mqComp = new EfficiencyOverviewResponse.MqComparison();
        mqComp.setTotal(buildComparisonItem(mqTotalPrevious, mqTotalCurrent));
        mqComp.setUsageCount(buildComparisonItem(compareMqUsage != null ? compareMqUsage.longValue() : 0L, currentMqUsage));
        mqStats.setComparison(mqComp);

        // Mock 工厂：同比/环比。total 场景 current=顶层总数，previous=截止上期结束的总数
        long mockTotalCurrent = mockFactoryStats.getTotal() != null ? mockFactoryStats.getTotal() : 0L;
        long mockTotalPrevious = computeMockFactoryTotalAsOf(compareEndDateTime);
        Long compareMockExec = computeMockExecutionCount(compareRequest);
        long currentMockExec = mockFactoryStats.getExecutionCount() != null ? mockFactoryStats.getExecutionCount() : 0L;
        EfficiencyOverviewResponse.MockFactoryComparison mockComp = new EfficiencyOverviewResponse.MockFactoryComparison();
        mockComp.setTotal(buildComparisonItem(mockTotalPrevious, mockTotalCurrent));
        mockComp.setExecutionCount(buildComparisonItem(compareMockExec != null ? compareMockExec : 0L, currentMockExec));
        mockFactoryStats.setComparison(mockComp);

        // 自动化总数：同比/环比。total 场景 current=顶层总数，previous=截止上期结束的总数
        long automationTotalCurrent = automationStats.getTotal() != null ? automationStats.getTotal() : 0L;
        long automationTotalPrevious = computeAutomationTotalAsOf(compareEndTs, request);
        EfficiencyOverviewResponse.AutomationComparison automationComp = new EfficiencyOverviewResponse.AutomationComparison();
        automationComp.setTotal(buildComparisonItem(automationTotalPrevious, automationTotalCurrent));
        automationStats.setComparison(automationComp);

        // 测试计划总数：同比/环比。total 场景 current=顶层总数，previous=截止上期结束的总数
        long testPlanTotalCurrent = testPlanStats.getTotal() != null ? testPlanStats.getTotal() : 0L;
        long testPlanTotalPrevious = computeTestPlanTotalAsOf(compareEndTs, request);
        EfficiencyOverviewResponse.TestPlanComparison testPlanComp = new EfficiencyOverviewResponse.TestPlanComparison();
        testPlanComp.setTotal(buildComparisonItem(testPlanTotalPrevious, testPlanTotalCurrent));
        testPlanStats.setComparison(testPlanComp);

        // 自动化运行：同比/环比（周期内数量，非 total 场景）
        long currentRunTotal = computeAutomationRunTotal(request);
        long currentRunSuccess = computeAutomationRunSuccessCount(request);
        long compareRunTotal = computeAutomationRunTotal(compareRequest);
        long compareRunSuccess = computeAutomationRunSuccessCount(compareRequest);
        EfficiencyOverviewResponse.AutomationRunComparison runComp = new EfficiencyOverviewResponse.AutomationRunComparison();
        runComp.setTotal(buildComparisonItem(compareRunTotal, currentRunTotal));
        runComp.setSuccessCount(buildComparisonItem(compareRunSuccess, currentRunSuccess));
        automationRunStats.setComparison(runComp);

        // 功能用例执行：同比/环比（周期内数量，按 last_exec_time 范围）
        long currentFcTotal = computeFunctionalCaseExecutionTotal(request);
        long currentFcSuccess = computeFunctionalCaseExecutionSuccessCount(request);
        long compareFcTotal = computeFunctionalCaseExecutionTotal(compareRequest);
        long compareFcSuccess = computeFunctionalCaseExecutionSuccessCount(compareRequest);
        EfficiencyOverviewResponse.FunctionalCaseExecutionComparison fcComp = new EfficiencyOverviewResponse.FunctionalCaseExecutionComparison();
        fcComp.setTotal(buildComparisonItem(compareFcTotal, currentFcTotal));
        fcComp.setSuccessCount(buildComparisonItem(compareFcSuccess, currentFcSuccess));
        functionalCaseExecutionStats.setComparison(fcComp);

        // AI 用例生成：同比/环比（周期内数量，按 create_time 范围）
        if (aiCaseStats != null) {
            long currentAiCount = computeAiCaseCountForRequest(request);
            long currentManualCount = computeManualCaseCountForRequest(request);
            long compareAiCount = computeAiCaseCountForRequest(compareRequest);
            long compareManualCount = computeManualCaseCountForRequest(compareRequest);
            long currentTotal = currentAiCount + currentManualCount;
            long compareTotal = compareAiCount + compareManualCount;
            double currentAiRatio = currentTotal > 0 ? Math.round(currentAiCount * 10000.0 / currentTotal) / 100.0 : 0.0;
            double compareAiRatio = compareTotal > 0 ? Math.round(compareAiCount * 10000.0 / compareTotal) / 100.0 : 0.0;
            EfficiencyOverviewResponse.AiCaseComparison aiComp = new EfficiencyOverviewResponse.AiCaseComparison();
            aiComp.setAiCaseCount(buildComparisonItem(compareAiCount, currentAiCount));
            aiComp.setManualCaseCount(buildComparisonItem(compareManualCount, currentManualCount));
            aiComp.setAiRatio(buildComparisonItem(Math.round(compareAiRatio), Math.round(currentAiRatio)));
            aiCaseStats.setComparison(aiComp);
        }
    }

    /**
     * 构建同比/环比单项：previous=上期，current=当期；delta=当期−上期；changeRate=(当期−上期)/上期×100%，上期为0时为null
     */
    private static EfficiencyOverviewResponse.ComparisonItem buildComparisonItem(long previous, long current) {
        long delta = current - previous;
        EfficiencyOverviewResponse.ComparisonItem item = new EfficiencyOverviewResponse.ComparisonItem();
        item.setCurrent(current);
        item.setPrevious(previous);
        item.setDelta(delta);
        item.setChangeType(toChangeType(delta));
        if (previous != 0) {
            item.setChangeRate(Math.round((delta * 100.0 / previous) * 100.0) / 100.0);
        } else {
            item.setChangeRate(null);
        }
        return item;
    }

    private static String toChangeType(long delta) {
        if (delta > 0) return "up";
        if (delta < 0) return "down";
        return "flat";
    }

    /**
     * 造数工厂总数（截止某时刻）：metadata_definition 表 create_time &lt;= endTs，用于 total 场景的 previous
     */
    private long computeDataFactoryTotalAsOf(long endTs, EfficiencyOverviewRequest request) {
        LambdaQueryWrapper<MetadataDefinition> wrapper = buildMetadataDefinitionQueryWrapperAsOf(request, "SCRIPT", endTs);
        Long count = metadataDefinitionMapper.selectCount(wrapper);
        return count != null ? count : 0L;
    }

    /**
     * MQ 总数（截止某时刻）：metadata_definition 表 create_time &lt;= endTs，用于 total 场景的 previous
     */
    private long computeMqTotalAsOf(long endTs, EfficiencyOverviewRequest request) {
        LambdaQueryWrapper<MetadataDefinition> wrapper = buildMetadataDefinitionQueryWrapperAsOf(request, "ROCKETMQ", endTs);
        Long count = metadataDefinitionMapper.selectCount(wrapper);
        return count != null ? count : 0L;
    }

    /**
     * Mock 工厂总数（截止某时刻）：spotter_runner.mock_scene_rule created_at &lt;= endTime，用于 total 场景的 previous
     */
    private long computeMockFactoryTotalAsOf(LocalDateTime endTime) {
        return mockSceneRuleMapper.countByDeletedAtIsNull(null, endTime);
    }

    /**
     * 自动化总数（截止某时刻）：workflow_definition 表 create_time &lt;= endTs，用于 total 场景的 previous
     */
    private long computeAutomationTotalAsOf(long endTs, EfficiencyOverviewRequest request) {
        LambdaQueryWrapper<WorkflowDefinition> wrapper = buildWorkflowDefinitionQueryWrapperAsOf(request, endTs);
        Long count = workflowDefinitionMapper.selectCount(wrapper);
        return count != null ? count : 0L;
    }

    /**
     * 测试计划总数（截止某时刻）：test_plan create_time &lt;= endTs，用于 total 场景的 previous
     */
    private long computeTestPlanTotalAsOf(long endTs, EfficiencyOverviewRequest request) {
        List<String> projectIds = (request.getProjectIds() != null && !request.getProjectIds().isEmpty())
                ? request.getProjectIds() : null;
        String createUser = StringUtils.isNotBlank(request.getUserId()) ? request.getUserId() : null;
        long count = testPlanStatsMapper.countDistinctGroupIdExcludingNone(projectIds, createUser, null, endTs);
        return count;
    }

    /**
     * 构建 metadata_definition 总数查询条件（仅 projectIds、userId，无日期）：用于概览总数及同环比 current
     */
    private LambdaQueryWrapper<MetadataDefinition> buildMetadataDefinitionQueryWrapperForTotal(
            EfficiencyOverviewRequest request, String protocol) {
        LambdaQueryWrapper<MetadataDefinition> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(MetadataDefinition::getProtocol, protocol)
               .isNull(MetadataDefinition::getDeletedTime);
        if (request.getProjectIds() != null && !request.getProjectIds().isEmpty()) {
            wrapper.in(MetadataDefinition::getProjectId, request.getProjectIds());
        }
        if (StringUtils.isNotBlank(request.getUserId())) {
            wrapper.eq(MetadataDefinition::getCreateUser, request.getUserId());
        }
        return wrapper;
    }

    /**
     * 构建 metadata_definition 查询条件：protocol、deleted_time IS NULL、projectIds、userId(create_user)、可选日期范围
     * 用于同比/环比等对比场景；metadata_definition 无 personal 字段，用 userId 对应 create_user；创建/修改时间为时间戳
     */
    private LambdaQueryWrapper<MetadataDefinition> buildMetadataDefinitionQueryWrapper(
            EfficiencyOverviewRequest request, String protocol) {
        LambdaQueryWrapper<MetadataDefinition> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(MetadataDefinition::getProtocol, protocol)
               .isNull(MetadataDefinition::getDeletedTime);
        if (request.getProjectIds() != null && !request.getProjectIds().isEmpty()) {
            wrapper.in(MetadataDefinition::getProjectId, request.getProjectIds());
        }
        if (StringUtils.isNotBlank(request.getUserId())) {
            wrapper.eq(MetadataDefinition::getCreateUser, request.getUserId());
        }
        if (StringUtils.isNotBlank(request.getStartDate()) && StringUtils.isNotBlank(request.getEndDate())) {
            LocalDate startDate = LocalDate.parse(request.getStartDate(), DateTimeFormatter.ISO_DATE);
            LocalDate endDate = LocalDate.parse(request.getEndDate(), DateTimeFormatter.ISO_DATE);
            long startTs = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endTs = endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            wrapper.ge(MetadataDefinition::getCreateTime, startTs)
                   .le(MetadataDefinition::getCreateTime, endTs);
        }
        return wrapper;
    }

    /**
     * 构建 metadata_definition 查询条件（截止某时刻）：create_time &lt;= endTs，用于 total 场景 previous
     */
    private LambdaQueryWrapper<MetadataDefinition> buildMetadataDefinitionQueryWrapperAsOf(
            EfficiencyOverviewRequest request, String protocol, long endTs) {
        LambdaQueryWrapper<MetadataDefinition> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(MetadataDefinition::getProtocol, protocol)
               .isNull(MetadataDefinition::getDeletedTime)
               .le(MetadataDefinition::getCreateTime, endTs);
        if (request.getProjectIds() != null && !request.getProjectIds().isEmpty()) {
            wrapper.in(MetadataDefinition::getProjectId, request.getProjectIds());
        }
        if (StringUtils.isNotBlank(request.getUserId())) {
            wrapper.eq(MetadataDefinition::getCreateUser, request.getUserId());
        }
        return wrapper;
    }

    /**
     * 计算造数提效总时长（小时）, 造数提效率
     * 老表：runner_operation_log (biz_type=1)
     * 新表：metadata_operation_log (biz_type=1, module_type='SCRIPT')
     */
    @SuppressWarnings("unchecked")
    private EfficiencyOverviewResponse.DataGenerationEfficiency computeDataGenerationEfficiency(
            EfficiencyOverviewRequest request) {
        
        // 构建查询条件
        LambdaQueryWrapper<MetadataOperationLog> queryWrapper = buildBaseQueryWrapper(request);
        queryWrapper.eq(MetadataOperationLog::getBizType, 1)
                   .eq(MetadataOperationLog::getModuleType, "SCRIPT")
                   .eq(MetadataOperationLog::getAction, "execute");
        
        List<MetadataOperationLog> logRows = metadataOperationLogMapper.selectList(queryWrapper);
        
        if (logRows == null || logRows.isEmpty()) {
            // 返回空结构，包含所有复杂度等级
            Map<String, EfficiencyOverviewResponse.SaveDetail> saveDetail = new HashMap<>();
            for (int i = 0; i < 7; i++) {
                EfficiencyOverviewResponse.SaveDetail detail = new EfficiencyOverviewResponse.SaveDetail();
                detail.setTotalSaveRatio(0.0);
                detail.setTotalSaveTime(0.0);
                saveDetail.put("D" + i, detail);
            }
            
            EfficiencyOverviewResponse.DataGenerationEfficiency result = 
                    new EfficiencyOverviewResponse.DataGenerationEfficiency();
            result.setTotalSaveRatio(0.0);
            result.setTotalSaveTime(0.0);
            result.setTotalEstimatedTime(0.0);
            result.setTotalSaveExecutionTime(0.0);
            result.setSaveDetail(saveDetail);
            result.setCallCount(new ArrayList<>());
            result.setComplexityDetail(new ArrayList<>());
            return result;
        }
        
        // 批量查询：收集所有唯一的 related_id（对应 script_id）
        Set<String> scriptIds = logRows.stream()
                .map(MetadataOperationLog::getRelatedId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        Map<String, ScriptManage> scriptMap = new HashMap<>();
        if (!scriptIds.isEmpty()) {
            LambdaQueryWrapper<ScriptManage> scriptWrapper = Wrappers.lambdaQuery();
            scriptWrapper.in(ScriptManage::getScriptId, scriptIds)
                        .isNull(ScriptManage::getDeletedTime);
            List<ScriptManage> scripts = scriptManageMapper.selectList(scriptWrapper);
            scriptMap = scripts.stream()
                    .collect(Collectors.toMap(ScriptManage::getScriptId, s -> s, (k1, k2) -> k1));
        }
        
        // 复杂度等级对应的基准预估时间（秒）
        Map<String, Integer> complexityBaseTime = new HashMap<>();
        complexityBaseTime.put("D0", 300);   // 简单：5分钟
        complexityBaseTime.put("D1", 600);   // 较简单：10分钟
        complexityBaseTime.put("D2", 1800);  // 中等：30分钟
        complexityBaseTime.put("D3", 3600);  // 较复杂：60分钟
        complexityBaseTime.put("D4", 5400);  // 复杂：90分钟
        complexityBaseTime.put("D5", 10800);  // 高：180分钟
        complexityBaseTime.put("D6", 27000);  // 极高：450分钟
        
        // 为每个唯一的 related_id 预先计算复杂度等级
        Map<String, String> complexityLevelMap = new HashMap<>();
        List<EfficiencyOverviewResponse.ComplexityDetail> complexityDetailList = new ArrayList<>();
        
        for (String scriptId : scriptIds) {
            ScriptManage script = scriptMap.get(scriptId);
            if (script == null) {
                continue;
            }
            
            String scriptContent = script.getScriptContent();
            Map<String, Object> complexityResult = CodeComplexityEvaluator.evaluateComplexity(scriptContent);
            String complexityLevel = (String) complexityResult.get("level");
            complexityLevelMap.put(scriptId, complexityLevel);
            
            // 构建造数复杂度等级明细（与 Python 参考一致：biz_name 空则 ""）
            EfficiencyOverviewResponse.ComplexityDetail detail =
                    new EfficiencyOverviewResponse.ComplexityDetail();
            detail.setBizName(script.getScriptName() != null ? script.getScriptName() : "");
            detail.setRelatedId(scriptId);
            detail.setScores((Map<String, Object>) complexityResult.get("scores"));
            detail.setTotalCs(((Number) complexityResult.get("total_cs")).doubleValue());
            detail.setLevel(complexityLevel);
            complexityDetailList.add(detail);
        }
        
        // 按复杂度等级统计
        Map<String, ComplexityStats> complexityStats = new HashMap<>();
        for (int i = 0; i < 7; i++) {
            complexityStats.put("D" + i, new ComplexityStats());
        }
        
        double totalEstimatedTime = 0.0;
        double totalSaveExecutionTime = 0.0;
        
        // 计算每个复杂度等级的提效时间和提效率
        for (MetadataOperationLog row : logRows) {
            if (row.getRelatedId() == null) {
                continue;
            }
            
            String scriptId = row.getRelatedId();
            String complexityLevel = complexityLevelMap.getOrDefault(scriptId, "D0");
            Integer baseEstimatedTime = complexityBaseTime.getOrDefault(complexityLevel, 600);
            
            double estTime = baseEstimatedTime;
            // 与 Python 参考一致：有实际耗时时用 execution_time_ms/1000，否则默认 10 秒
            double saveTime = (row.getExecutionTimeMs() != null && row.getExecutionTimeMs() > 0)
                    ? row.getExecutionTimeMs() / 1000.0 : 10.0;

            // 累加到对应复杂度等级
            ComplexityStats stats = complexityStats.get(complexityLevel);
            stats.estimatedTime += estTime;
            stats.saveExecutionTime += saveTime;
            
            // 累加到总体
            totalEstimatedTime += estTime;
            totalSaveExecutionTime += saveTime;
        }
        
        // 计算每个复杂度等级的提效时间和提效率
        Map<String, EfficiencyOverviewResponse.SaveDetail> saveDetail = new HashMap<>();
        for (Map.Entry<String, ComplexityStats> entry : complexityStats.entrySet()) {
            String complexityLevel = entry.getKey();
            ComplexityStats stats = entry.getValue();
            
            EfficiencyOverviewResponse.SaveDetail detail = new EfficiencyOverviewResponse.SaveDetail();
            if (stats.estimatedTime > 0) {
                double complexitySaveTime = (stats.estimatedTime - stats.saveExecutionTime) / 3600.0;
                double complexitySaveRatio = ((stats.estimatedTime - stats.saveExecutionTime) / stats.estimatedTime) * 100.0;
                detail.setTotalSaveTime(Math.round(complexitySaveTime * 100.0) / 100.0);
                detail.setTotalSaveRatio(Math.round(complexitySaveRatio * 100.0) / 100.0);
            } else {
                detail.setTotalSaveTime(0.0);
                detail.setTotalSaveRatio(0.0);
            }
            saveDetail.put(complexityLevel, detail);
        }
        
        // 计算总体提效时间和提效率
        double totalEstimatedTimeHours = totalEstimatedTime / 3600.0;
        double totalSaveExecutionTimeHours = totalSaveExecutionTime / 3600.0;
        
        double totalSaveTime = 0.0;
        double totalSaveRatio = 0.0;
        if (totalEstimatedTime > 0) {
            totalSaveTime = (totalEstimatedTime - totalSaveExecutionTime) / 3600.0;
            totalSaveRatio = ((totalEstimatedTime - totalSaveExecutionTime) / totalEstimatedTime) * 100.0;
        }
        
        // 查询调用次数：通过 related_id 分组，按 count 倒序取前十
        List<EfficiencyOverviewResponse.CallCountItem> callCountList = getCallCountList(request, scriptMap);
        
        EfficiencyOverviewResponse.DataGenerationEfficiency result = 
                new EfficiencyOverviewResponse.DataGenerationEfficiency();
        result.setTotalSaveRatio(Math.round(totalSaveRatio * 100.0) / 100.0);
        result.setTotalSaveTime(Math.round(totalSaveTime * 100.0) / 100.0);
        result.setTotalEstimatedTime(Math.round(totalEstimatedTimeHours * 100.0) / 100.0);
        result.setTotalSaveExecutionTime(Math.round(totalSaveExecutionTimeHours * 100.0) / 100.0);
        result.setSaveDetail(saveDetail);
        result.setCallCount(callCountList);
        result.setComplexityDetail(complexityDetailList);
        
        return result;
    }

    /**
     * 计算MQ使用次数
     * 老表：runner_operation_log (biz_type=1, action='send')
     * 新表：metadata_operation_log (biz_type=1, module_type='ROCKETMQ', action='execute')
     */
    private Integer computeMqUsageCount(EfficiencyOverviewRequest request) {
        LambdaQueryWrapper<MetadataOperationLog> queryWrapper = buildBaseQueryWrapper(request);
        queryWrapper.eq(MetadataOperationLog::getBizType, 1)
                   .eq(MetadataOperationLog::getModuleType, "ROCKETMQ")
                   .eq(MetadataOperationLog::getAction, "execute");
        
        Long count = metadataOperationLogMapper.selectCount(queryWrapper);
        return count != null ? count.intValue() : 0;
    }

    /**
     * 计算工具采纳度（可指定日期范围；可选传入活跃用户数，用于 /activity 时含用例执行与自动化用户）
     */
    private EfficiencyOverviewResponse.ToolAdoptionRate computeToolAdoptionRate(
            EfficiencyOverviewRequest request, LocalDate startDate, LocalDate endDate) {
        return computeToolAdoptionRate(request, startDate, endDate, null);
    }

    private EfficiencyOverviewResponse.ToolAdoptionRate computeToolAdoptionRate(
            EfficiencyOverviewRequest request, LocalDate startDate, LocalDate endDate, Integer activeUserCountOverride) {
        int activeUserCount;
        if (activeUserCountOverride != null) {
            activeUserCount = activeUserCountOverride;
        } else {
            LambdaQueryWrapper<MetadataOperationLog> queryWrapper = Wrappers.lambdaQuery();
            if (startDate != null && endDate != null) {
                LocalDateTime startDateTime = startDate.atStartOfDay();
                LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
                queryWrapper.ge(MetadataOperationLog::getCreatedAt, startDateTime)
                        .le(MetadataOperationLog::getCreatedAt, endDateTime);
            } else {
                queryWrapper = buildDateQueryWrapper(request);
            }
            if (request.getProjectIds() != null && !request.getProjectIds().isEmpty()) {
                queryWrapper.in(MetadataOperationLog::getProjectId, request.getProjectIds());
            }
            List<MetadataOperationLog> logs = metadataOperationLogMapper.selectList(queryWrapper);
            Set<String> activeUsers = logs.stream()
                    .map(MetadataOperationLog::getUserEmail)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            activeUserCount = activeUsers.size();
        }
        int targetUserCount = 150;
        double adoptionRate = 0.0;
        if (targetUserCount > 0) {
            adoptionRate = (activeUserCount * 100.0) / targetUserCount;
        }
        EfficiencyOverviewResponse.ToolAdoptionRate result =
                new EfficiencyOverviewResponse.ToolAdoptionRate();
        result.setActiveUserCount(activeUserCount);
        result.setTargetUserCount(targetUserCount);
        result.setAdoptionRate(Math.round(adoptionRate * 100.0) / 100.0);
        return result;
    }

    /**
     * 查询用户活跃度（时间范围内总数，不按日拆分；默认近5天）
     */
    public UserActivityResponse getUserActivityLast5Days(EfficiencyOverviewRequest request) {
        LocalDate startDate;
        LocalDate endDate;
        if (StringUtils.isNotBlank(request.getStartDate()) &&
                StringUtils.isNotBlank(request.getEndDate())) {
            startDate = LocalDate.parse(request.getStartDate(), DateTimeFormatter.ISO_DATE);
            endDate = LocalDate.parse(request.getEndDate(), DateTimeFormatter.ISO_DATE);
        } else {
            // 默认使用近5天
            endDate = LocalDate.now();
            startDate = endDate.minusDays(4); // 近5天，包括当天
        }
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        LambdaQueryWrapper<MetadataOperationLog> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.ge(MetadataOperationLog::getCreatedAt, startDateTime)
                   .le(MetadataOperationLog::getCreatedAt, endDateTime)
                   .notIn(MetadataOperationLog::getAction, Arrays.asList("view", "query"));
        
        if (request.getPersonal() != null && !request.getPersonal().isEmpty()) {
            queryWrapper.in(MetadataOperationLog::getUserEmail, request.getPersonal());
        }
        if (request.getProjectIds() != null && !request.getProjectIds().isEmpty()) {
            queryWrapper.in(MetadataOperationLog::getProjectId, request.getProjectIds());
        }

        List<MetadataOperationLog> activeLogs = metadataOperationLogMapper.selectList(queryWrapper);

        // 先汇总 Case Execution、Automation 按邮箱，再与 metadata_operation_log 用户聚合为统一用户列表
        long startTs = startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endTs = endDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        List<String> caseExecExecuteUserIds = null;
        if (request.getPersonal() != null && !request.getPersonal().isEmpty()) {
            List<User> usersByEmail = baseUserMapper.selectUserIdByEmailList(request.getPersonal());
            caseExecExecuteUserIds = usersByEmail.stream().map(User::getId).collect(Collectors.toList());
            if (caseExecExecuteUserIds.isEmpty()) {
                caseExecExecuteUserIds = Arrays.asList("__none__");
            }
        }
        List<String> caseExecUserIdsRaw = testPlanFunctionalCaseActivityMapper.selectExecuteUserIdsInRange(
                startTs, endTs,
                request.getProjectIds() != null && !request.getProjectIds().isEmpty() ? request.getProjectIds() : null,
                caseExecExecuteUserIds);
        Map<String, Long> userIdToCaseExecCount = caseExecUserIdsRaw.stream()
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.groupingBy(u -> u, Collectors.counting()));
        Map<String, String> caseExecUserIdToEmail = new HashMap<>();
        if (!userIdToCaseExecCount.isEmpty()) {
            UserExample userExample = new UserExample();
            userExample.createCriteria().andIdIn(new ArrayList<>(userIdToCaseExecCount.keySet()));
            List<User> users = userMapper.selectByExample(userExample);
            caseExecUserIdToEmail = users.stream().collect(Collectors.toMap(User::getId, u -> u.getEmail() != null ? u.getEmail() : u.getId(), (a, b) -> a));
        }
        Map<String, Integer> caseExecCountByEmail = new HashMap<>();
        for (Map.Entry<String, Long> e : userIdToCaseExecCount.entrySet()) {
            String email = caseExecUserIdToEmail.getOrDefault(e.getKey(), e.getKey());
            caseExecCountByEmail.merge(email, e.getValue().intValue(), Integer::sum);
        }
        LambdaQueryWrapper<WorkflowRun> runWrapper = Wrappers.lambdaQuery();
        runWrapper.isNull(WorkflowRun::getDeletedTime)
                .ge(WorkflowRun::getCreateTime, startTs)
                .le(WorkflowRun::getCreateTime, endTs);
        if (request.getProjectIds() != null && !request.getProjectIds().isEmpty()) {
            runWrapper.in(WorkflowRun::getProjectId, request.getProjectIds());
        }
        if (request.getPersonal() != null && !request.getPersonal().isEmpty()) {
            List<User> usersByEmail = baseUserMapper.selectUserIdByEmailList(request.getPersonal());
            List<String> userIds = usersByEmail.stream().map(User::getId).collect(Collectors.toList());
            if (!userIds.isEmpty()) {
                runWrapper.in(WorkflowRun::getTriggerUser, userIds);
            } else {
                runWrapper.eq(WorkflowRun::getTriggerUser, "__none__");
            }
        }
        List<WorkflowRun> runs = workflowRunMapper.selectList(runWrapper);
        Map<String, Long> userIdToRunCount = runs.stream()
                .filter(r -> StringUtils.isNotBlank(r.getTriggerUser()))
                .collect(Collectors.groupingBy(WorkflowRun::getTriggerUser, Collectors.counting()));
        Map<String, String> automationUserIdToEmail = new HashMap<>();
        if (!userIdToRunCount.isEmpty()) {
            UserExample userExample = new UserExample();
            userExample.createCriteria().andIdIn(new ArrayList<>(userIdToRunCount.keySet()));
            List<User> users = userMapper.selectByExample(userExample);
            automationUserIdToEmail = users.stream().collect(Collectors.toMap(User::getId, u -> u.getEmail() != null ? u.getEmail() : u.getId(), (a, b) -> a));
        }
        Map<String, Integer> automationCountByEmail = new HashMap<>();
        for (Map.Entry<String, Long> e : userIdToRunCount.entrySet()) {
            String email = automationUserIdToEmail.getOrDefault(e.getKey(), e.getKey());
            automationCountByEmail.merge(email, e.getValue().intValue(), Integer::sum);
        }

        // 统一用户列表：metadata_operation_log + Case Execution + Automation 的并集，保证两个维度 item 数量一致
        Set<String> unifiedUserEmailsSet = new LinkedHashSet<>();
        activeLogs.stream().map(MetadataOperationLog::getUserEmail).filter(Objects::nonNull).forEach(unifiedUserEmailsSet::add);
        caseExecCountByEmail.keySet().forEach(unifiedUserEmailsSet::add);
        automationCountByEmail.keySet().forEach(unifiedUserEmailsSet::add);
        List<String> unifiedUserEmails = new ArrayList<>(unifiedUserEmailsSet);
        Collections.sort(unifiedUserEmails);

        // series 1: 用户 + bizType 维度，Case Execution 与 Automation 计入 Web
        List<UserActivityResponse.UserSeriesItem> seriesByBizTypeItems = new ArrayList<>();
        for (String email : unifiedUserEmails) {
            String userName = email.contains("@") ? email.split("@")[0] : email;
            Map<String, Integer> breakdown = new LinkedHashMap<>();
            int webFromLogs = 0, pluginFromLogs = 0, electronFromLogs = 0;
            for (MetadataOperationLog log : activeLogs) {
                if (!email.equals(log.getUserEmail())) continue;
                String btName = mapBizTypeToName(log.getBizType());
                if ("Web".equals(btName)) webFromLogs++;
                else if ("Plugin".equals(btName)) pluginFromLogs++;
                else if ("Electron".equals(btName)) electronFromLogs++;
            }
            int caseExecCount = caseExecCountByEmail.getOrDefault(email, 0);
            int automationCount = automationCountByEmail.getOrDefault(email, 0);
            int webTotal = webFromLogs + caseExecCount + automationCount;
            breakdown.put("Web", webTotal);
            breakdown.put("Plugin", pluginFromLogs);
            breakdown.put("Electron", electronFromLogs);
            long totalActivity = webTotal + pluginFromLogs + electronFromLogs;
            UserActivityResponse.UserSeriesItem item = new UserActivityResponse.UserSeriesItem();
            item.setUser(userName);
            item.setTotalActivity(totalActivity);
            item.setBreakdown(breakdown);
            seriesByBizTypeItems.add(item);
        }
        seriesByBizTypeItems.sort(Comparator.comparing(UserActivityResponse.UserSeriesItem::getUser));

        UserActivityResponse.SeriesGroup seriesBizType = new UserActivityResponse.SeriesGroup();
        seriesBizType.setDimension("bizType");
        seriesBizType.setDescription("用户+业务类型维度(Web/Plugin/Electron)");
        seriesBizType.setItems(seriesByBizTypeItems);

        // series 2: 用户 + module_type 维度，与 bizType 使用同一用户列表
        List<UserActivityResponse.UserSeriesItem> seriesByModuleTypeItems = new ArrayList<>();
        for (String email : unifiedUserEmails) {
            String userName = email.contains("@") ? email.split("@")[0] : email;
            Map<String, Integer> breakdown = new LinkedHashMap<>();
            long[] userTotal = {0L};
            activeLogs.stream()
                    .filter(log -> email.equals(log.getUserEmail()))
                    .forEach(log -> {
                        String mtName = mapModuleTypeToName(log.getModuleType());
                        breakdown.merge(mtName, 1, Integer::sum);
                        userTotal[0]++;
                    });
            int caseExecCount = caseExecCountByEmail.getOrDefault(email, 0);
            if (caseExecCount > 0) {
                breakdown.put("Case Execution", caseExecCount);
                userTotal[0] += caseExecCount;
            }
            int automationCount = automationCountByEmail.getOrDefault(email, 0);
            if (automationCount > 0) {
                breakdown.put("Automation", automationCount);
                userTotal[0] += automationCount;
            }
            UserActivityResponse.UserSeriesItem item = new UserActivityResponse.UserSeriesItem();
            item.setUser(userName);
            item.setTotalActivity(userTotal[0]);
            item.setBreakdown(breakdown);
            seriesByModuleTypeItems.add(item);
        }
        seriesByModuleTypeItems.sort(Comparator.comparing(UserActivityResponse.UserSeriesItem::getUser));

        UserActivityResponse.SeriesGroup seriesModuleType = new UserActivityResponse.SeriesGroup();
        seriesModuleType.setDimension("moduleType");
        seriesModuleType.setDescription("用户+模块类型维度");
        seriesModuleType.setItems(seriesByModuleTypeItems);

        List<UserActivityResponse.SeriesGroup> series = Arrays.asList(seriesBizType, seriesModuleType);

        // totalActivity = 埋点活跃数 + 用例执行次数 + 自动化运行次数
        int totalActivitySum = activeLogs.size() + caseExecUserIdsRaw.size() + runs.size();
        // toolAdoptionRate 活跃用户数 = 统一用户列表人数（含仅有用例执行/自动化的用户）
        EfficiencyOverviewResponse.ToolAdoptionRate toolAdoptionRate =
                computeToolAdoptionRate(request, startDate, endDate, unifiedUserEmails.size());

        UserActivityResponse response = new UserActivityResponse();
        response.setSeries(series);
        response.setTotalActivity(totalActivitySum);
        response.setToolAdoptionRate(toolAdoptionRate);
        response.setOnlineUserStats(onlineUserService.getOnlineUserStats());
        return response;
    }

    /**
     * 构建基础查询条件
     */
    private LambdaQueryWrapper<MetadataOperationLog> buildBaseQueryWrapper(
            EfficiencyOverviewRequest request) {
        LambdaQueryWrapper<MetadataOperationLog> queryWrapper = buildDateQueryWrapper(request);

        // 添加用户过滤条件（personal 支持字符串或数组）
        if (request.getPersonal() != null && !request.getPersonal().isEmpty()) {
            queryWrapper.in(MetadataOperationLog::getUserEmail, request.getPersonal());
        }

        // 项目搜索：按项目筛选 projectIds: ["id1","id2"]
        if (request.getProjectIds() != null && !request.getProjectIds().isEmpty()) {
            queryWrapper.in(MetadataOperationLog::getProjectId, request.getProjectIds());
        }

        return queryWrapper;
    }

    /**
     * 构建日期查询条件
     */
    private LambdaQueryWrapper<MetadataOperationLog> buildDateQueryWrapper(
            EfficiencyOverviewRequest request) {
        LambdaQueryWrapper<MetadataOperationLog> queryWrapper = Wrappers.lambdaQuery();
        
        if (StringUtils.isNotBlank(request.getStartDate()) && 
            StringUtils.isNotBlank(request.getEndDate())) {
            LocalDate startDate = LocalDate.parse(request.getStartDate(), DateTimeFormatter.ISO_DATE);
            LocalDate endDate = LocalDate.parse(request.getEndDate(), DateTimeFormatter.ISO_DATE);
            
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
            
            queryWrapper.ge(MetadataOperationLog::getCreatedAt, startDateTime)
                       .le(MetadataOperationLog::getCreatedAt, endDateTime);
        }
        
        return queryWrapper;
    }

    /**
     * 获取调用次数列表
     */
    private List<EfficiencyOverviewResponse.CallCountItem> getCallCountList(
            EfficiencyOverviewRequest request, Map<String, ScriptManage> scriptMap) {
        
        LambdaQueryWrapper<MetadataOperationLog> queryWrapper = buildBaseQueryWrapper(request);
        queryWrapper.eq(MetadataOperationLog::getBizType, 1)
                   .eq(MetadataOperationLog::getModuleType, "SCRIPT")
                   .eq(MetadataOperationLog::getAction, "execute");
        
        List<MetadataOperationLog> logs = metadataOperationLogMapper.selectList(queryWrapper);
        
        // 按 related_id 分组统计
        Map<String, Long> callCountMap = logs.stream()
                .filter(log -> log.getRelatedId() != null)
                .collect(Collectors.groupingBy(
                        MetadataOperationLog::getRelatedId,
                        Collectors.counting()
                ));
        
        // 按调用次数倒序，取前十
        List<EfficiencyOverviewResponse.CallCountItem> callCountList = callCountMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(entry -> {
                    EfficiencyOverviewResponse.CallCountItem item = 
                            new EfficiencyOverviewResponse.CallCountItem();
                    item.setRelatedId(entry.getKey());
                    ScriptManage script = scriptMap.get(entry.getKey());
                    item.setBizName(script != null ? script.getScriptName() : "");
                    item.setCallCount(entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
        
        return callCountList;
    }

    /**
     * bizType 映射为客户端名称：1=Web, 2=Plugin, 3=Electron
     */
    private String mapBizTypeToName(Integer bizType) {
        if (bizType == null) return "OTHER";
        switch (bizType) {
            case 1: return "Web";
            case 2: return "Plugin";
            case 3: return "Electron";
            default: return "OTHER";
        }
    }

    /**
     * 直接返回 module_type，不做映射
     */
    private String mapModuleTypeToName(String moduleType) {
        return StringUtils.isBlank(moduleType) ? "OTHER" : moduleType;
    }

    /**
     * 复杂度统计内部类
     */
    private static class ComplexityStats {
        double estimatedTime = 0.0;
        double saveExecutionTime = 0.0;
    }
}
