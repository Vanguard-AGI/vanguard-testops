package io.vanguard.testops.functional.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 用例效能指标DTO（返回给前端）
 */
@Data
public class CaseMetricsDTO {

    @Schema(description = "项目ID")
    private String projectId;

    @Schema(description = "快照日期")
    private Long snapshotDate;

    @Schema(description = "统计时间范围-开始")
    private Long timeRangeStart;

    @Schema(description = "统计时间范围-结束")
    private Long timeRangeEnd;

    // ========== 时间指标 (2个) ==========
    
    @Schema(description = "用例平均CS复杂分")
    private BigDecimal avgCaseCS;

    @Schema(description = "用例产出率")
    private BigDecimal caseOutputRate;

    // ========== 行为指标 (7个) ==========
    
    @Schema(description = "用例变更热度")
    private Integer caseChangeHeat;

    @Schema(description = "高价值用例执行热度（%）")
    private BigDecimal highValueExecRate;

    @Schema(description = "测试工时节约率（%）")
    private BigDecimal timeSavingRate;

    @Schema(description = "测试计划用例复用率（%）")
    private BigDecimal planCaseReuseRate;

    @Schema(description = "测试计划用例修改率（%）")
    private BigDecimal planCaseModifyRate;

    @Schema(description = "测试计划用例新增率（%）")
    private BigDecimal planCaseNewRate;

    @Schema(description = "测试计划平均用例执行时长（毫秒）")
    private Long planAvgExecDuration;

    // ========== 质量指标 (2个) ==========
    
    @Schema(description = "测试计划用例通过率（%）")
    private BigDecimal planPassRate;

    @Schema(description = "测试计划首次通过率（%）")
    private BigDecimal planFirstPassRate;

    // ========== 额外统计数据 ==========
    
    @Schema(description = "总用例数")
    private Integer totalCaseCount;

    @Schema(description = "总CS分值")
    private BigDecimal totalCS;

    @Schema(description = "高频用例数")
    private Integer highFreqCaseCount;

    @Schema(description = "完成用例数")
    private Integer completedCaseCount;
}

