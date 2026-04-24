package io.vanguard.testops.requirementquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 需求质量详情 - 复用指标（按需求下测试计划组内用例聚合，与效能大屏口径一致）
 */
@Data
public class RequirementQualityReuseMetricsVO {

    @Schema(description = "复用用例数")
    private Long reusedCaseCount;

    @Schema(description = "总用例数")
    private Long totalCaseCount;

    @Schema(description = "复用用例 CS 分值之和")
    private BigDecimal reusedCsTotal;

    @Schema(description = "总 CS 分值")
    private BigDecimal totalCsTotal;

    @Schema(description = "绝对节省时间（毫秒，由 case_metrics_detail.saved_write_ms 汇总）")
    private Long absoluteTimeSavingsMs;
}
