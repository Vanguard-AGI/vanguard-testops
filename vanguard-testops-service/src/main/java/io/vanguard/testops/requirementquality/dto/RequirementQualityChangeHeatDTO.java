package io.vanguard.testops.requirementquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 需求质量详情 - 变更热度指标（与效能大屏口径一致：用例新增率、用例变更热度及分子分母）
 */
@Data
public class RequirementQualityChangeHeatDTO {

    @Schema(description = "用例新增率(%)")
    private Double caseIncreaseRate;

    @Schema(description = "新增用例数（分子）")
    private Long newCases;

    @Schema(description = "已有/期初用例数（分母）")
    private Long existingCases;

    @Schema(description = "用例变更热度(%)")
    private Double caseChangeHeat;

    @Schema(description = "修正用例数（分子）")
    private Long modifiedCases;

    @Schema(description = "总用例数（变更热度分母）")
    private Long totalCases;
}
