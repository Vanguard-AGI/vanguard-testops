package io.vanguard.testops.requirementquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 需求质量详情 - 工时指标按复杂度分级（与效能大屏口径一致，L1=基础/L2=中等/L3=复杂/L4=高难度）
 */
@Data
public class RequirementQualityWorkHourByLevelVO {

    @Schema(description = "预期编写时长-基础(L1)，分钟")
    private BigDecimal expectedWriteMinutesL1;
    @Schema(description = "预期编写时长-中等(L2)，分钟")
    private BigDecimal expectedWriteMinutesL2;
    @Schema(description = "预期编写时长-复杂(L3)，分钟")
    private BigDecimal expectedWriteMinutesL3;
    @Schema(description = "预期编写时长-高难度(L4)，分钟")
    private BigDecimal expectedWriteMinutesL4;

    @Schema(description = "预期执行时长-基础(L1)，分钟")
    private BigDecimal expectedExecMinutesL1;
    @Schema(description = "预期执行时长-中等(L2)，分钟")
    private BigDecimal expectedExecMinutesL2;
    @Schema(description = "预期执行时长-复杂(L3)，分钟")
    private BigDecimal expectedExecMinutesL3;
    @Schema(description = "预期执行时长-高难度(L4)，分钟")
    private BigDecimal expectedExecMinutesL4;

    @Schema(description = "实际编写时长合计，分钟")
    private BigDecimal actualWriteMinutesTotal;
    @Schema(description = "实际执行时长合计，分钟")
    private BigDecimal actualExecMinutesTotal;
}
