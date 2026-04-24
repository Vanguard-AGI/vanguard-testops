package io.vanguard.testops.requirementquality.dto;

import lombok.Data;

/**
 * 需求质量概览聚合结果（需求→测试计划→用例 全局汇总）
 */
@Data
public class RequirementQualityOverviewAggVO {
    private Long caseTotalCount;
    private Long caseExecutedCount;
    private Long passedCount;
    /** 首次通过数：各需求下 (plan_id, case_id) 取最早一条执行结果为通过的用例数之和，与 benefitMetrics 一致 */
    private Long firstPassCount;
    /** 总执行用例数：case_execution_record 中 distinct (plan_id, case_id) 之和，与 benefitMetrics 口径一致，作为首次通过率分母 */
    private Long totalExecutedCountFromCer;
    /** 首次通过率平均值：仅对有该指标（totalExecutedCountFromCer>0）的需求算 100*firstPassCount/totalExecutedCountFromCer 后取平均，与千行代码缺陷率口径一致 */
    private Double avgFirstPassRate;
    /** 千行代码缺陷率平均值：仅对 (frontend_loc_changed+backend_loc_changed)>0 的需求算 (缺陷总数/变更行数)*1000 后取平均 */
    private Double avgDefectRatePer1k;
}
