package io.vanguard.testops.requirementquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 需求质量详情 - 其它效益指标（UQS、首次通过率等）查询结果 VO
 */
@Data
public class RequirementQualityBenefitMetricsVO {

    @Schema(description = "平均UQS评分")
    private BigDecimal avgUqsScore;

    @Schema(description = "首次执行通过用例数（first_exec_result 为 SUCCESS/PASS）")
    private Long firstPassCount;

    @Schema(description = "有首次执行结果的用例数（分母）")
    private Long totalExecutedCount;
}
