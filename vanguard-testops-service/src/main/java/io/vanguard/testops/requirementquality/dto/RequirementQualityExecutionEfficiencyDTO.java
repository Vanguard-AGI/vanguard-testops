package io.vanguard.testops.requirementquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 需求质量详情 - 执行效率指标（与效能大屏口径一致：平均用例执行时长、手动用例执行热度及分子分母）
 */
@Data
public class RequirementQualityExecutionEfficiencyDTO {

    @Schema(description = "平均用例执行时长(分钟)")
    private Double avgExecutionTime;

    @Schema(description = "执行总长累计(分钟)")
    private Double totalExecutionTime;

    @Schema(description = "执行次数")
    private Long executionCount;

    @Schema(description = "手动用例执行热度(%)")
    private Double manualExecutionHeat;

    @Schema(description = "高频回归用例总分（分子，执行次数>=3的用例CS分之和）")
    private Double highFreqRegressionScore;

    @Schema(description = "所有用例总分（分母）")
    private Double totalCaseScore;
}
