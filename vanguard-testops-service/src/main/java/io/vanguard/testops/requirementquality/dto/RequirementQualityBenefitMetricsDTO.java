package io.vanguard.testops.requirementquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 需求质量详情 - 其它效益指标（返回给前端：平均UQS、首次通过率等）
 */
@Data
public class RequirementQualityBenefitMetricsDTO {

    @Schema(description = "平均UQS评分")
    private Double avgUQSScore;

    @Schema(description = "验证发现率（UQS 子项，暂无单独数据源时可为 0）")
    private Double verificationDiscoveryRate;

    @Schema(description = "可执行率（UQS 子项，暂无单独数据源时可为 0）")
    private Double executabilityRate;

    @Schema(description = "复用率(%)，与复用指标一致")
    private Double reuseRate;

    @Schema(description = "首次通过率(%)")
    private Double firstPassRate;

    @Schema(description = "首次执行通过用例数")
    private Long firstPassCount;

    @Schema(description = "总执行用例数（有首次执行结果的用例数）")
    private Long totalExecutionCount;
}
