package io.vanguard.testops.requirementquality.mapper;

import io.vanguard.testops.requirementquality.dto.RequirementQualityCaseExecutionRowVO;
import io.vanguard.testops.requirementquality.dto.RequirementQualityExecutorContributionVO;
import io.vanguard.testops.requirementquality.dto.RequirementQualityExecutionTrendVO;
import io.vanguard.testops.requirementquality.dto.RequirementQualityListRowVO;
import io.vanguard.testops.requirementquality.dto.RequirementQualityOverviewAggVO;
import io.vanguard.testops.requirementquality.dto.RequirementQualityReasonDistributionVO;
import io.vanguard.testops.requirementquality.dto.RequirementQualityBenefitMetricsVO;
import io.vanguard.testops.requirementquality.dto.RequirementQualityReuseMetricsVO;
import io.vanguard.testops.requirementquality.dto.RequirementQualityWorkHourByLevelVO;
import io.vanguard.testops.requirementquality.dto.StoryLocDeployAggVO;
import io.vanguard.testops.system.dto.sdk.OptionDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 需求质量视图 - 扩展 Mapper
 * 按 story_id 聚合 meego_story_stats + test_plan(feishu_story_id) + 用例/执行数据
 */
public interface ExtRequirementQualityMapper {

    /**
     * 分页查询需求质量列表（含测试计划聚合的用例数、执行率、通过率、执行周期、工时偏差）
     */
    List<RequirementQualityListRowVO> selectRequirementQualityListPage(
            @Param("projectIds") List<String> projectIds,
            @Param("status") String status,
            @Param("storyIds") List<String> storyIds,
            @Param("executionPeriodStart") Long executionPeriodStart,
            @Param("executionPeriodEnd") Long executionPeriodEnd,
            @Param("sortBy") String sortBy,
            @Param("sortOrder") String sortOrder,
            @Param("offset") long offset,
            @Param("pageSize") int pageSize);

    /**
     * 统计符合条件的需求总数（用于分页 total）
     */
    long countRequirementQualityList(
            @Param("projectIds") List<String> projectIds,
            @Param("status") String status,
            @Param("storyIds") List<String> storyIds,
            @Param("executionPeriodStart") Long executionPeriodStart,
            @Param("executionPeriodEnd") Long executionPeriodEnd);

    /**
     * 概览卡全局聚合：与列表同筛选条件，汇总总用例数、已执行数、通过数（需求→测试计划→用例）
     */
    RequirementQualityOverviewAggVO selectRequirementQualityOverviewAgg(
            @Param("projectIds") List<String> projectIds,
            @Param("status") String status,
            @Param("storyIds") List<String> storyIds,
            @Param("executionPeriodStart") Long executionPeriodStart,
            @Param("executionPeriodEnd") Long executionPeriodEnd);

    /**
     * 筛选项：有关联测试计划组的项目 ID 列表
     */
    List<String> selectProjectIdsWithLinkedGroup();

    /**
     * 筛选项：已关联测试计划组的需求（id=story_id, name=story_name）
     */
    List<OptionDTO> selectRequirementOptionsWithLinkedGroup();

    /**
     * 需求库关键词搜索：从完整需求库 meego_story_stats 按 story_id / story_name 模糊匹配（供门禁补全弹窗）
     */
    List<OptionDTO> selectStorySearchByKeyword(@Param("keyword") String keyword);

    /**
     * 根据需求 ID 列表批量查需求名称（id=story_id, name=story_name，供门禁列表展示）
     */
    List<OptionDTO> selectStoryNamesByIds(@Param("storyIds") List<String> storyIds);

    /**
     * 需求详情 - 用例执行明细（按需求下组→计划→用例聚合，含执行次数、耗时、最后结果；无 metrics 时用 tpfc 近似）
     */
    List<RequirementQualityCaseExecutionRowVO> selectCaseExecutionDetailByStoryId(@Param("storyId") String storyId);

    /**
     * 需求详情 - 执行人贡献度（按需求下计划→执行记录聚合，每个执行人执行的去重用例数，按用例数降序）
     */
    List<RequirementQualityExecutorContributionVO> selectExecutorContributionByStoryId(@Param("storyId") String storyId);

    /**
     * 需求详情 - 测试用例执行阻塞原因分布（需求下计划→用例→test_plan_case_metrics.block_reason 统计）
     */
    List<RequirementQualityReasonDistributionVO> selectBlockReasonDistributionByStoryId(@Param("storyId") String storyId);

    /**
     * 需求详情 - 用例变更原因分布（需求下计划→用例→case_change_log.change_reason 统计）
     */
    List<RequirementQualityReasonDistributionVO> selectChangeReasonDistributionByStoryId(@Param("storyId") String storyId);

    /**
     * 需求详情 - 用例执行趋势（按日统计通过/失败/阻塞，最近 30 天，需求下计划→case_execution_record）
     */
    List<RequirementQualityExecutionTrendVO> selectExecutionTrendByStoryId(@Param("storyId") String storyId);

    /**
     * 需求详情 - 用例优先级分布（需求下计划→用例→functional_priority 自定义字段统计）
     */
    List<RequirementQualityReasonDistributionVO> selectPriorityDistributionByStoryId(@Param("storyId") String storyId);

    /**
     * 需求详情 - 复用指标（需求下测试计划组内用例，与效能大屏口径一致：case_metrics_detail 复用/CS/节省工时）
     */
    RequirementQualityReuseMetricsVO selectReuseMetricsByStoryId(@Param("storyId") String storyId);

    /**
     * 需求详情 - 工时按复杂度分级（预期编写/执行 L1-L4；单条返回各等级汇总，需配合实际编写/执行总时长）
     */
    List<java.util.Map<String, Object>> selectExpectedTimeByLevelByStoryId(@Param("storyId") String storyId);

    /**
     * 需求详情 - 实际编写时长（需求关联的 meego_story_stats.test_analysis_time，单位转为分钟）
     */
    Long selectActualWriteMinutesByStoryId(@Param("storyId") String storyId);

    /**
     * 需求详情 - 实际执行时长（需求下测试计划组内 test_plan_metrics 汇总，单位毫秒，调用方转分钟）
     */
    Long selectActualExecMsByStoryId(@Param("storyId") String storyId);

    /**
     * 需求详情 - 其它效益指标：平均UQS（由 Service 按公式计算）、首次通过数/总执行数（从 case_execution_record 按 plan_id+case_id 取最早执行结果）
     */
    RequirementQualityBenefitMetricsVO selectBenefitMetricsByStoryId(@Param("storyId") String storyId);

    /**
     * 需求详情 - UQS 子项原始数据（total_exec_cases, total_defects, total_exec_count, success_without_block_count），用于 Service 计算 UQS 与验证发现率/可执行率
     */
    java.util.Map<String, Object> selectBenefitUqsRawByStoryId(@Param("storyId") String storyId);

    /**
     * 需求详情 - 变更热度原始数据（与效能大屏口径：新增数/期初数、修正数/总用例数），按需求下测试计划组聚合
     */
    java.util.Map<String, Object> selectChangeHeatRawByStoryId(@Param("storyId") String storyId);

    /**
     * 需求详情 - 执行效率原始数据（与效能大屏口径：总执行时长/执行次数、高频CS总分/总CS总分），按需求下测试计划组聚合
     */
    java.util.Map<String, Object> selectExecutionEfficiencyRawByStoryId(@Param("storyId") String storyId);

    /**
     * 按 story_id 从 requirement_change_stats 聚合行数与发布指标（用于流水线关联需求时写回 meego_story_stats）
     */
    StoryLocDeployAggVO selectStoryLocDeployAgg(@Param("storyId") String storyId);

    /**
     * 将聚合结果写回 meego_story_stats（仅更新已存在的 story_id 行）
     */
    int updateMeegoStoryStatsLocDeploy(
            @Param("storyId") String storyId,
            @Param("frontendLocChanged") Integer frontendLocChanged,
            @Param("backendLocChanged") Integer backendLocChanged,
            @Param("deployTotalCount") Integer deployTotalCount,
            @Param("deployFailureCount") Integer deployFailureCount,
            @Param("lastDeployTime") Long lastDeployTime,
            @Param("changeFailureRate") java.math.BigDecimal changeFailureRate);
}
