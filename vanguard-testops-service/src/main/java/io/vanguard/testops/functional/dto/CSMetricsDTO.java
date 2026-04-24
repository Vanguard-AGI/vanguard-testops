package io.vanguard.testops.functional.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * CS指标响应DTO
 */
@Data
public class CSMetricsDTO {

    @Schema(description = "用例平均CS复杂分")
    private BigDecimal avgCaseCS = BigDecimal.ZERO;

    @Schema(description = "用例产出率（用例数/总CS分）")
    private BigDecimal caseOutputRate = BigDecimal.ZERO;

    @Schema(description = "用例变更热度（被修改用例数）")
    private Integer caseChangeHeat = 0;

    @Schema(description = "高价值用例执行热度（%）")
    private BigDecimal highValueExecRate = BigDecimal.ZERO;

    @Schema(description = "测试工时节约率（%）")
    private BigDecimal timeSavingRate = BigDecimal.ZERO;

    @Schema(description = "测试计划用例复用率（%）")
    private BigDecimal planCaseReuseRate = BigDecimal.ZERO;

    @Schema(description = "测试计划用例修改率（%）")
    private BigDecimal planCaseModifyRate = BigDecimal.ZERO;

    @Schema(description = "测试计划用例新增率（%）")
    private BigDecimal planCaseNewRate = BigDecimal.ZERO;

    @Schema(description = "测试计划平均用例执行时长（分钟）")
    private BigDecimal planAvgExecDuration = BigDecimal.ZERO;

    @Schema(description = "测试计划用例通过率（%）")
    private BigDecimal planPassRate = BigDecimal.ZERO;

    @Schema(description = "测试计划首次通过率（%）")
    private BigDecimal planFirstPassRate = BigDecimal.ZERO;

    @Schema(description = "总用例数")
    private Integer totalCaseCount = 0;

    @Schema(description = "总CS分值")
    private BigDecimal totalCS = BigDecimal.ZERO;

    @Schema(description = "高频用例数量")
    private Integer highFreqCaseCount = 0;

    @Schema(description = "完成用例数")
    private Integer completedCaseCount = 0;
}

