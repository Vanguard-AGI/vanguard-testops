package io.vanguard.testops.functional.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 效率仪概览响应DTO
 */
@Data
@Schema(description = "效率仪概览响应")
public class EfficiencyOverviewResponse {

    @Schema(description = "造数提效数据")
    private DataGenerationEfficiency dataGenerationEfficiency;

    @Schema(description = "造数工厂：总数来自 metadata_definition(protocol=SCRIPT)，执行数量来自 metadata_operation_log")
    private DataFactoryStats dataFactoryStats;

    @Schema(description = "MQ：总数来自 metadata_definition(protocol=ROCKETMQ)，使用次数来自 metadata_operation_log")
    private MqStats mqStats;

    @Schema(description = "Mock工厂：总数来自 spotter_runner.mock_scene_rule(deleted_at IS NULL)，执行总数来自 metadata_operation_log(module_type=MOCK, action=execute)")
    private MockFactoryStats mockFactoryStats;

    @Schema(description = "自动化总数：来自 workflow_definition(deleted_time IS NULL)，查询条件与 metadata_definition 一致：projectIds、userId(create_user)、create_time 时间范围")
    private AutomationStats automationStats;

    @Schema(description = "测试计划总数：来自 spotter_aegis.test_plan，按 group_id 去重且排除 NONE，查询条件同 metadata_definition")
    private TestPlanStats testPlanStats;

    @Schema(description = "自动化运行数据：来自 spotter_aegis.workflow_run，trigger_user/create_time/deleted_time IS NULL，含运行总数与成功率")
    private AutomationRunStats automationRunStats;

    @Schema(description = "功能用例执行数据：来自 spotter_aegis.test_plan_functional_case 关联 test_plan，支持 projectIds/userId/时间，含执行总数与成功率（last_exec_result=SUCCESS）")
    private FunctionalCaseExecutionStats functionalCaseExecutionStats;

    @Schema(description = "工具采纳度")
    private ToolAdoptionRate toolAdoptionRate;

    @Schema(description = "AI 用例生成指标：来自 functional_case，统计时间范围内新增的 AI/手工用例及占比、AI 新增率")
    private AiCaseStats aiCaseStats;

    @Schema(description = "同比/环比维度，仅当请求带 comparisonType 且已计算对比时返回：YOY=同比，MOM=环比")
    private String comparisonType;
    @Schema(description = "对比期日期范围（如 2025-01-22~2025-01-28），仅当有对比时返回")
    private String comparisonPeriod;
    @Schema(description = "对比维度展示文案：同比（与上一年同期对比）/ 环比（与上一统计周期对比），仅当有对比时返回")
    private String comparisonLabel;

    /**
     * 同比/环比单项对比数据（专业结构）。变化率公式：(当期−上期)/上期×100%；total 场景下 current=截止当前查询截止时间的总数、previous=截止上期截止时间的总数
     */
    @Data
    @Schema(description = "同比/环比单项对比。current=当期、previous=上期；delta=当期−上期；changeRate=(当期−上期)/上期×100%，上期为0时为null。total 场景：current/previous 为截止该期结束时的累计总数。")
    public static class ComparisonItem {
        @Schema(description = "当期数值（total 场景为截止当前查询截止时间的总数，否则为当前周期内数量）")
        private Long current;
        @Schema(description = "上期数值（total 场景为截止上期截止时间的总数，否则为上周期内数量）")
        private Long previous;
        @Schema(description = "变化量：当期 − 上期，正=增量负=减量")
        private Long delta;
        @Schema(description = "变化类型：up=上升/绿色箭头，down=下降/红色箭头，flat=持平")
        private String changeType;
        @Schema(description = "变化率（%），公式：(当期−上期)/上期×100%，保留2位小数；上期为0时为null")
        private Double changeRate;
    }

    @Data
    @Schema(description = "造数工厂统计")
    public static class DataFactoryStats {
        @Schema(description = "造数工厂总数（metadata_definition protocol=SCRIPT）")
        private Long total;
        @Schema(description = "造数工厂执行数量（metadata_operation_log）")
        private Long executionCount;
        @Schema(description = "同比/环比对比数据，仅当请求带 comparisonType 时返回")
        private DataFactoryComparison comparison;
    }

    @Data
    @Schema(description = "造数工厂同比/环比")
    public static class DataFactoryComparison {
        @Schema(description = "总数对比")
        private ComparisonItem total;
        @Schema(description = "执行数对比")
        private ComparisonItem executionCount;
    }

    @Data
    @Schema(description = "MQ统计")
    public static class MqStats {
        @Schema(description = "MQ总数（metadata_definition protocol=ROCKETMQ）")
        private Long total;
        @Schema(description = "MQ使用次数（metadata_operation_log）")
        private Long usageCount;
        @Schema(description = "同比/环比对比数据，仅当请求带 comparisonType 时返回")
        private MqComparison comparison;
    }

    @Data
    @Schema(description = "MQ同比/环比")
    public static class MqComparison {
        @Schema(description = "总数对比")
        private ComparisonItem total;
        @Schema(description = "使用次数对比")
        private ComparisonItem usageCount;
    }

    @Data
    @Schema(description = "Mock工厂统计")
    public static class MockFactoryStats {
        @Schema(description = "Mock工厂总数（spotter_runner.mock_scene_rule deleted_at IS NULL）")
        private Long total;
        @Schema(description = "Mock工厂执行总数（metadata_operation_log module_type=MOCK, action=execute）")
        private Long executionCount;
        @Schema(description = "同比/环比对比数据，仅当请求带 comparisonType 时返回")
        private MockFactoryComparison comparison;
    }

    @Data
    @Schema(description = "Mock工厂同比/环比")
    public static class MockFactoryComparison {
        @Schema(description = "总数对比")
        private ComparisonItem total;
        @Schema(description = "执行数对比")
        private ComparisonItem executionCount;
    }

    @Data
    @Schema(description = "自动化统计（workflow_definition 表，查询条件同 metadata_definition）")
    public static class AutomationStats {
        @Schema(description = "自动化工作流总数（workflow_definition deleted_time IS NULL）")
        private Long total;
        @Schema(description = "同比/环比对比数据，仅当请求带 comparisonType 时返回")
        private AutomationComparison comparison;
    }

    @Data
    @Schema(description = "自动化同比/环比")
    public static class AutomationComparison {
        @Schema(description = "总数对比")
        private ComparisonItem total;
    }

    @Data
    @Schema(description = "测试计划统计（spotter_aegis.test_plan，按 group_id 去重且排除 NONE，查询条件同 metadata_definition）")
    public static class TestPlanStats {
        @Schema(description = "测试计划总数（COUNT DISTINCT group_id，排除 group_id=NONE）")
        private Long total;
        @Schema(description = "同比/环比对比数据，仅当请求带 comparisonType 时返回")
        private TestPlanComparison comparison;
    }

    @Data
    @Schema(description = "测试计划同比/环比")
    public static class TestPlanComparison {
        @Schema(description = "总数对比")
        private ComparisonItem total;
    }

    @Data
    @Schema(description = "自动化运行统计（spotter_aegis.workflow_run，trigger_user/create_time，deleted_time IS NULL）")
    public static class AutomationRunStats {
        @Schema(description = "自动化运行总数")
        private Long total;
        @Schema(description = "成功率（%），SUCCESS 数/总数×100，保留2位小数；总数为0时为null")
        private Double successRate;
        @Schema(description = "同比/环比对比数据，仅当请求带 comparisonType 时返回")
        private AutomationRunComparison comparison;
    }

    @Data
    @Schema(description = "自动化运行同比/环比")
    public static class AutomationRunComparison {
        @Schema(description = "运行总数对比")
        private ComparisonItem total;
        @Schema(description = "成功数对比（status=SUCCESS）")
        private ComparisonItem successCount;
    }

    @Data
    @Schema(description = "功能用例执行统计（spotter_aegis.test_plan_functional_case 关联 test_plan，projectIds/userId/last_exec_time）")
    public static class FunctionalCaseExecutionStats {
        @Schema(description = "功能用例执行总数（有 last_exec_time 且在时间范围内的记录）")
        private Long total;
        @Schema(description = "成功率（%），last_exec_result=SUCCESS 数/总数×100，保留2位小数；总数为0时为null")
        private Double successRate;
        @Schema(description = "同比/环比对比数据，仅当请求带 comparisonType 时返回")
        private FunctionalCaseExecutionComparison comparison;
    }

    @Data
    @Schema(description = "功能用例执行同比/环比")
    public static class FunctionalCaseExecutionComparison {
        @Schema(description = "执行总数对比")
        private ComparisonItem total;
        @Schema(description = "成功数对比（last_exec_result=SUCCESS）")
        private ComparisonItem successCount;
    }

    @Data
    @Schema(description = "造数提效数据")
    public static class DataGenerationEfficiency {
        @Schema(description = "总提效率（%）")
        private Double totalSaveRatio;

        @Schema(description = "总提效时长（小时）")
        private Double totalSaveTime;

        @Schema(description = "总预估时长（小时）")
        private Double totalEstimatedTime;

        @Schema(description = "总实际执行时长（小时）")
        private Double totalSaveExecutionTime;

        @Schema(description = "按复杂度等级分组的提效详情")
        private Map<String, SaveDetail> saveDetail;

        @Schema(description = "调用次数列表")
        private List<CallCountItem> callCount;

        @Schema(description = "复杂度明细列表")
        private List<ComplexityDetail> complexityDetail;
    }

    @Data
    @Schema(description = "提效详情")
    public static class SaveDetail {
        @Schema(description = "提效率（%）")
        private Double totalSaveRatio;

        @Schema(description = "提效时长（小时）")
        private Double totalSaveTime;
    }

    @Data
    @Schema(description = "调用次数项")
    public static class CallCountItem {
        @Schema(description = "关联业务ID")
        private String relatedId;

        @Schema(description = "业务名称")
        private String bizName;

        @Schema(description = "调用次数")
        private Long callCount;
    }

    @Data
    @Schema(description = "复杂度明细")
    public static class ComplexityDetail {
        @Schema(description = "业务名称")
        private String bizName;

        @Schema(description = "关联业务ID")
        private String relatedId;

        @Schema(description = "评分详情")
        private Map<String, Object> scores;

        @Schema(description = "总复杂度分数")
        private Double totalCs;

        @Schema(description = "复杂度等级（D0-D6）")
        private String level;
    }

    @Data
    @Schema(description = "工具采纳度")
    public static class ToolAdoptionRate {
        @Schema(description = "活跃用户数")
        private Integer activeUserCount;

        @Schema(description = "目标用户数")
        private Integer targetUserCount;

        @Schema(description = "采纳率（%）")
        private Double adoptionRate;
    }

    @Data
    @Schema(description = "AI 用例生成指标：时间范围内新增的 AI/手工用例及占比、AI 新增率")
    public static class AiCaseStats {
        @Schema(description = "AI 生成的用例数（时间范围内新增，ai_create=true）")
        private Long aiCaseCount;
        @Schema(description = "手工创建的用例数（时间范围内新增，ai_create=false）")
        private Long manualCaseCount;
        @Schema(description = "AI 用例占比（%）= aiCaseCount / 总新增 × 100，总新增为 0 时为 null")
        private Double aiRatio;
        @Schema(description = "手工用例占比（%）= manualCaseCount / 总新增 × 100，总新增为 0 时为 null")
        private Double manualRatio;
        @Schema(description = "AI 用例新增率（%）= 本期新增 AI 用例数 / 本期新增总用例数 × 100，与 aiRatio 一致，总新增为 0 时为 null")
        private Double aiCaseNewRate;
        @Schema(description = "同比/环比对比数据，仅当请求带 comparisonType 时返回")
        private AiCaseComparison comparison;
    }

    @Data
    @Schema(description = "AI 用例生成同比/环比")
    public static class AiCaseComparison {
        @Schema(description = "AI 用例数对比")
        private ComparisonItem aiCaseCount;
        @Schema(description = "手工用例数对比")
        private ComparisonItem manualCaseCount;
        @Schema(description = "AI 占比对比（current/previous 为百分比数值，如 25 表示 25%）")
        private ComparisonItem aiRatio;
    }
}
