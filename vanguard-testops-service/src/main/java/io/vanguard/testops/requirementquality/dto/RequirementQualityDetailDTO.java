package io.vanguard.testops.requirementquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 需求质量视图 - 详情（系分 6.4，当前仅 6.4.1 概览块）
 */
@Data
public class RequirementQualityDetailDTO {

    @Schema(description = "需求ID")
    private String storyId;

    @Schema(description = "需求名称")
    private String storyName;

    @Schema(description = "负责人，来自测试计划 create_user")
    private String owner;

    @Schema(description = "状态，来自测试计划 status")
    private String status;

    @Schema(description = "执行周期开始时间戳(毫秒)，与列表一致")
    private Long executionPeriodStart;

    @Schema(description = "执行周期结束时间戳(毫秒)，与列表一致")
    private Long executionPeriodEnd;

    @Schema(description = "项目ID")
    private String projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "迭代ID")
    private String iterationId;

    @Schema(description = "迭代名称")
    private String iterationName;

    @Schema(description = "总用例数")
    private Long caseTotalCount;

    @Schema(description = "已执行用例数")
    private Long caseExecutedCount;

    @Schema(description = "执行率，0-100")
    private Double executionRate;

    @Schema(description = "通过率，0-100")
    private Double passRate;

    @Schema(description = "用例执行明细（排名、失败/成功次数、耗时、失败率等）")
    private java.util.List<RequirementQualityCaseExecutionRowDTO> caseExecutionList;

    @Schema(description = "执行人贡献度（执行人姓名及执行的用例数，按用例数降序）")
    private java.util.List<RequirementQualityExecutorContributionDTO> executorContributionList;

    @Schema(description = "测试用例执行阻塞原因分布（名称+数量，按数量降序）")
    private java.util.List<RequirementQualityReasonDistributionDTO> blockReasonDistribution;

    @Schema(description = "用例变更原因分布（名称+数量，按数量降序）")
    private java.util.List<RequirementQualityReasonDistributionDTO> changeReasonDistribution;

    @Schema(description = "用例执行趋势（按日：日期、通过/失败/阻塞数、通过率，按日期升序）")
    private java.util.List<RequirementQualityExecutionTrendDTO> executionTrendList;

    @Schema(description = "用例优先级分布（名称+数量，如 P0-核心流程）")
    private java.util.List<RequirementQualityReasonDistributionDTO> priorityDistribution;

    @Schema(description = "复用指标（用例数量/工作量复用率、绝对节省时间，需求下测试计划组内用例聚合）")
    private RequirementQualityReuseMetricsDTO reuseMetrics;

    @Schema(description = "工时指标按复杂度分级（预期/实际编写与执行时长，需求下测试计划组内用例聚合）")
    private RequirementQualityWorkHourByLevelVO workHourByLevel;

    @Schema(description = "工时偏差指标（整体）：编写/执行的实际与理论工时及偏差率")
    private RequirementQualityWorkHourDeviationDTO workHourDeviation;

    @Schema(description = "其它效益指标：平均UQS、验证发现率/可执行率/复用率、首次通过率及分子分母")
    private RequirementQualityBenefitMetricsDTO benefitMetrics;

    @Schema(description = "变更热度指标（与效能大屏口径：用例新增率、用例变更热度及分子分母）")
    private RequirementQualityChangeHeatDTO changeHeatMetrics;

    @Schema(description = "执行效率指标（与效能大屏口径：平均用例执行时长、手动用例执行热度及分子分母）")
    private RequirementQualityExecutionEfficiencyDTO executionEfficiencyMetrics;

    @Schema(description = "代码覆盖率(%)，直接取库")
    private Double codeCoverage;

    @Schema(description = "前端缺陷率(千行代码)")
    private Double frontendDefectRate;

    @Schema(description = "后端缺陷率(千行代码)")
    private Double backendDefectRate;

    @Schema(description = "总千行代码缺陷率")
    private Double totalDefectRatePer1k;

    @Schema(description = "前端缺陷数，meego_story_stats.frontend_defect_count")
    private Integer frontendDefectCount;

    @Schema(description = "后端缺陷数，meego_story_stats.backend_defect_count")
    private Integer backendDefectCount;

    @Schema(description = "前端变更代码行数，meego_story_stats.frontend_loc_changed")
    private Integer frontendLocChanged;

    @Schema(description = "后端变更代码行数，meego_story_stats.backend_loc_changed")
    private Integer backendLocChanged;

    @Schema(description = "变更失败率(%)")
    private Double changeFailureRate;

    @Schema(description = "变更成功率(%)，(成功发布数/总发布数)×100")
    private Double changeSuccessRate;

    @Schema(description = "总发布次数")
    private Integer deployTotalCount;

    @Schema(description = "失败次数(回滚+紧急补丁)")
    private Integer deployFailureCount;
}
