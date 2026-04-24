package io.vanguard.testops.requirementquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 需求质量详情 - 工时偏差指标（整体）：编写/执行的实际与理论工时及偏差率
 */
@Data
public class RequirementQualityWorkHourDeviationDTO {

    @Schema(description = "编写工时偏差率(%)")
    private Double writingDeviationRate;

    @Schema(description = "实际编写工时(小时)")
    private Double actualWritingHours;

    @Schema(description = "理论编写工时(小时)")
    private Double theoreticalWritingHours;

    @Schema(description = "执行工时偏差率(%)")
    private Double executionDeviationRate;

    @Schema(description = "实际执行工时(分钟)")
    private Double actualExecutionMinutes;

    @Schema(description = "理论执行工时(分钟)")
    private Double theoreticalExecutionMinutes;
}
