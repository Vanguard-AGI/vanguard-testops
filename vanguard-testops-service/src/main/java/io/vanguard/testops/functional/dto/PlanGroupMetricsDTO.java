package io.vanguard.testops.functional.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 测试计划组维度指标 DTO（按计划组聚合的新增率、变更率）
 * 接口传计划 id，后端解析计划组后按计划组统计
 */
@Data
public class PlanGroupMetricsDTO {

    @Schema(description = "计划 id（传入的 planId）")
    private String planId;

    @Schema(description = "计划组 id")
    private String groupId;

    @Schema(description = "计划组内用例总数（新增+复用）")
    private Long totalCaseCount;

    @Schema(description = "计划组内本次新增来源用例数")
    private Long newCaseCount;

    @Schema(description = "计划组内变更次数（去重用例数）")
    private Long modifiedCaseCount;

    @Schema(description = "测试计划新增率（%） = 本次新增用例数 / 测试计划用例总数 × 100")
    private BigDecimal planGroupNewRate;

    @Schema(description = "测试计划变更率（%） = 测试计划变更次数 / 测试计划用例总数 × 100")
    private BigDecimal planGroupModifyRate;
}
