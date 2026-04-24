package io.vanguard.testops.plan.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class TestPlanMetrics implements Serializable {
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "测试计划ID")
    private String testPlanId;

    @Schema(description = "项目ID")
    private String projectId;

    @Schema(description = "环境因子: 稳定1.0 / 不稳定1.5")
    private BigDecimal envInstabilityFactor;

    @Schema(description = "总用例数")
    private Integer totalCases;

    @Schema(description = "首次通过率%")
    private BigDecimal firstPassRate;

    @Schema(description = "平均复杂度CS")
    private BigDecimal avgCsScore;

    @Schema(description = "【算法理论】计划总编写工时")
    private Long totalAlgoWriteMs;

    @Schema(description = "【算法理论】计划总执行工时")
    private Long totalAlgoExecMs;

    @Schema(description = "编写时长偏差率（%）")
    private BigDecimal writeDeviationRate;

    @Schema(description = "复用总节约工时(小时)")
    private BigDecimal reuseSavingHours;

    @Schema(description = "总缺陷数 - 从飞书需求读取")
    private Integer totalDefectCount;

    @Schema(description = "开始时间")
    private Long startTime;

    @Schema(description = "结束时间")
    private Long endTime;

    @Schema(description = "最后计算时间")
    private Long lastCalcTime;

    @Schema(description = "【平台实测】计划总执行工时(毫秒)")
    private Long totalActualExecMs;

    @Schema(description = "【平台实测】计划总阅读工时(毫秒)")
    private Long totalActualReadingMs;

    @Schema(description = "【平台实测】计划总耗时(毫秒) = total_actual_exec_ms + total_actual_reading_ms")
    private Long totalActualTimeMs;

    @Schema(description = "平均执行时长偏差率(%)")
    private BigDecimal avgExecDeviationRate;

    @Schema(description = "用例复杂度方差")
    private BigDecimal complexityVariance;

    private static final long serialVersionUID = 1L;
}
