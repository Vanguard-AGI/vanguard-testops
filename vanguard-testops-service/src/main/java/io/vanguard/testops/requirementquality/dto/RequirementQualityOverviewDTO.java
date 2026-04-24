package io.vanguard.testops.requirementquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 需求质量视图 - 概览卡（系分 6.3）
 */
@Data
public class RequirementQualityOverviewDTO {

    @Schema(description = "本期需求数（符合筛选条件）")
    private long requirementTotal;

    @Schema(description = "总用例数（需求→测试计划→用例 全局汇总）")
    private Long caseTotalCount;

    @Schema(description = "已执行用例数")
    private Long caseExecutedCount;

    @Schema(description = "执行率 0-100")
    private Double executionRate;

    @Schema(description = "通过率 0-100")
    private Double passRate;

    @Schema(description = "首次通过率 0-100（与列表/详情一致：首次执行通过数/已执行数）")
    private Double firstPassRate;

    @Schema(description = "平均编写工时偏差率(%)，流水线/测分就绪后填充")
    private Double avgWriteDeviationRate;

    @Schema(description = "平均执行工时偏差率(%)，流水线/测分就绪后填充")
    private Double avgExecDeviationRate;

    @Schema(description = "千行代码缺陷率平均值：有该率的需求的 totalDefectRatePer1k 相加/数量，符合筛选的全局聚合")
    private Double avgDefectRatePer1k;
}
