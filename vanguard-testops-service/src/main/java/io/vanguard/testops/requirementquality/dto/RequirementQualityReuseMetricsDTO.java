package io.vanguard.testops.requirementquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 需求质量详情 - 复用指标（返回给前端，与效能大屏展示一致）
 */
@Data
public class RequirementQualityReuseMetricsDTO {

    @Schema(description = "用例数量复用率(%)")
    private Double reuseRateByCount;

    @Schema(description = "用例工作量复用率(%)")
    private Double reuseRateByWorkload;

    @Schema(description = "绝对节省时间(小时)")
    private Double absoluteTimeSavingsHours;

    @Schema(description = "复用用例数")
    private Long reusedCaseCount;

    @Schema(description = "总用例数")
    private Long totalCaseCount;

    @Schema(description = "复用用例总CS分值")
    private BigDecimal reusedCsTotal;

    @Schema(description = "总CS分值")
    private BigDecimal totalCsTotal;
}
