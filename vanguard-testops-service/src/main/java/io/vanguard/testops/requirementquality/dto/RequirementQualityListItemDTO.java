package io.vanguard.testops.requirementquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 需求质量视图 - 列表行 DTO（与系分 6.2 响应字段对齐）
 */
@Data
public class RequirementQualityListItemDTO {

    @Schema(description = "需求ID")
    private String storyId;

    @Schema(description = "需求名称")
    private String storyName;

    @Schema(description = "负责人，来自测试计划 create_user")
    private String owner;

    @Schema(description = "状态，来自测试计划 status：未开始/进行中/已完成/已归档")
    private String status;

    @Schema(description = "项目ID")
    private String projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "迭代ID")
    private String iterationId;

    @Schema(description = "迭代名称")
    private String iterationName;

    @Schema(description = "总用例数")
    private Long caseTotalCount;

    @Schema(description = "已执行用例数")
    private Long caseExecutedCount;

    @Schema(description = "执行率，0-100")
    private Double executionRate;

    @Schema(description = "通过率，0-100")
    private Double passRate;

    @Schema(description = "首次通过率，0-100（与详情一致：首次执行通过数/已执行数）")
    private Double firstPassRate;

    @Schema(description = "执行周期开始时间戳(ms)")
    private Long executionPeriodStart;

    @Schema(description = "执行周期结束时间戳(ms)")
    private Long executionPeriodEnd;

    @Schema(description = "平均编写工时偏差率(%)")
    private Double avgWriteDeviationRate;

    @Schema(description = "平均执行工时偏差率(%)")
    private Double avgExecDeviationRate;

    @Schema(description = "缺陷数，来自需求库 meego_story_stats.defect_count")
    private Integer defectCount;

    @Schema(description = "重开率(%)，来自需求库 meego_story_stats.reopen_rate，空则前端展示 -")
    private Double reopenRate;

    @Schema(description = "代码覆盖率(%)，直接取库 meego_story_stats.code_coverage，空则前端展示 -")
    private Double codeCoverage;

    @Schema(description = "前端缺陷率(千行代码)，(前端缺陷数/前端变更行数)×1000，分母为0时为空")
    private Double frontendDefectRate;

    @Schema(description = "后端缺陷率(千行代码)，(后端缺陷数/后端变更行数)×1000，分母为0时为空")
    private Double backendDefectRate;

    @Schema(description = "总千行代码缺陷率，(缺陷总数/变更代码行数)×1000，分母为0时为空")
    private Double totalDefectRatePer1k;

    @Schema(description = "变更失败率(%)，(回滚+紧急补丁)/总发布次数×100，直接取库或计算")
    private Double changeFailureRate;

    @Schema(description = "变更成功率(%)，(成功发布数/总发布数)×100，总发布数为0时为空")
    private Double changeSuccessRate;

    @Schema(description = "总发布次数，关联该需求的流水线数")
    private Integer deployTotalCount;

    @Schema(description = "失败次数(回滚+紧急补丁)")
    private Integer deployFailureCount;
}
