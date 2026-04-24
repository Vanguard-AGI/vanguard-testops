package io.vanguard.testops.requirementquality.dto;

import lombok.Data;

/**
 * 需求质量列表行（Mapper 查询结果，与 XML resultType 对应）
 */
@Data
public class RequirementQualityListRowVO {
    private String storyId;
    private String storyName;
    /** 负责人，来自 test_plan.create_user */
    private String owner;
    /** 测试计划状态，来自 test_plan.status：未开始/进行中/已完成/已归档 */
    private String planStatus;
    private Long caseTotalCount;
    private Long caseExecutedCount;
    private Long passedCount;
    /** 首次通过数：从 case_execution_record 按 (plan_id, case_id) 取最早一条执行结果为通过的用例数 */
    private Long firstPassCount;
    /** 总执行用例数：case_execution_record 中 distinct (plan_id, case_id)，与 benefitMetrics 口径一致，作为首次通过率分母 */
    private Long totalExecutedCountFromCer;
    private Long executionPeriodStart;
    private Long executionPeriodEnd;
    private java.math.BigDecimal avgWriteDeviationRate;
    private java.math.BigDecimal avgExecDeviationRate;
    /** 缺陷数，直接取 meego_story_stats.defect_count */
    private Integer defectCount;
    /** 重开率(%)，直接取 meego_story_stats.reopen_rate */
    private java.math.BigDecimal reopenRate;
    /** 代码覆盖率(%)，直接取 meego_story_stats.code_coverage */
    private java.math.BigDecimal codeCoverage;
    /** 前端缺陷数，meego_story_stats.frontend_defect_count */
    private Integer frontendDefectCount;
    /** 后端缺陷数，meego_story_stats.backend_defect_count */
    private Integer backendDefectCount;
    /** 前端变更行数，流水线聚合写入 meego_story_stats.frontend_loc_changed */
    private Integer frontendLocChanged;
    /** 后端变更行数，流水线聚合写入 meego_story_stats.backend_loc_changed */
    private Integer backendLocChanged;
    /** 总发布次数，meego_story_stats.deploy_total_count */
    private Integer deployTotalCount;
    /** 失败次数(回滚+紧急补丁)，meego_story_stats.deploy_failure_count */
    private Integer deployFailureCount;
    /** 变更失败率(%)，meego_story_stats.change_failure_rate */
    private java.math.BigDecimal changeFailureRate;
}
