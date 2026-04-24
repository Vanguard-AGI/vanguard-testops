package io.vanguard.testops.functional.domain;
import lombok.Data;


import java.io.Serializable;

import java.time.LocalDate;
import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

/**
* 用例效能指标数据表（基于CS评分体系）
* @TableName case_metrics_detail
*/
@Data
public class CaseMetricsData implements Serializable {

    /**
    * 主键ID
    */
    @Schema(description = "主键ID")
    private String id;
    /**
    * 项目ID
    */
    @Schema(description = "项目ID")
    private String projectId;
    /**
    * 测试计划ID（计划级指标时填写）
    */
    @Schema(description = "测试计划ID（计划级指标时填写）")
    private String planId;
    /**
    * 用例ID（用例级指标时填写）
    */
    @Schema(description = "用例ID（用例级指标时填写）")
    private String caseId;
    /**
    * 用户ID（按人统计时填写）
    */
    @Schema(description = "用户ID（按人统计时填写）")
    private String userId;
    /**
    * 指标级别：PROJECT-项目, PLAN-计划, CASE-用例, USER-用户
    */
    @Schema(description = "指标级别：PROJECT-项目, PLAN-计划, CASE-用例, USER-用户")
    private String metricLevel;
    /**
    * 时间维度：DAY-日, WEEK-周, MONTH-月, QUARTER-季, YEAR-年
    */
    @Schema(description = "时间维度：DAY-日, WEEK-周, MONTH-月, QUARTER-季, YEAR-年")
    private String timeDimension;
    /**
    * 快照日期
    */
    @Schema(description = "快照日期")
    private LocalDate snapshotDate;
    /**
    * 统计时间范围-开始（毫秒时间戳）
    */
    @Schema(description = "统计时间范围-开始（毫秒时间戳）")
    private Long timeRangeStart;
    /**
    * 统计时间范围-结束（毫秒时间戳）
    */
    @Schema(description = "统计时间范围-结束（毫秒时间戳）")
    private Long timeRangeEnd;
    /**
    * 单个用例的CS总分
    */
    @Schema(description = "单个用例的CS总分")
    private BigDecimal caseCsScore;
    /**
    * 总CS分值（多个用例汇总）
    */
    @Schema(description = "总CS分值（多个用例汇总）")
    private BigDecimal totalCsScore;
    /**
    * 平均CS分值
    */
    @Schema(description = "平均CS分值")
    private BigDecimal avgCsScore;
    /**
    * 用例认知复杂度得分（C1+C2）
    */
    @Schema(description = "用例认知复杂度得分（C1+C2）")
    private BigDecimal cognitiveScore;
    /**
    * 前置条件复杂度得分（C3+C4）
    */
    @Schema(description = "前置条件复杂度得分（C3+C4）")
    private BigDecimal preconditionScore;
    /**
    * 步骤细节复杂度得分（C5+C6+C7）
    */
    @Schema(description = "步骤细节复杂度得分（C5+C6+C7）")
    private BigDecimal stepDetailScore;
    /**
    * C1：用例标题长度得分（分段函数计算）
    */
    @Schema(description = "C1：用例标题长度得分（分段函数计算）")
    private BigDecimal csFactorC1;
    /**
    * C2：风险等级得分（P0/P1为1，其他为0）
    */
    @Schema(description = "C2：风险等级得分（P0/P1为1，其他为0）")
    private BigDecimal csFactorC2;
    /**
    * C3：前置条件数量
    */
    @Schema(description = "C3：前置条件数量")
    private BigDecimal csFactorC3;
    /**
    * C4：复杂数据准备（SQL/数据库关键词检测）
    */
    @Schema(description = "C4：复杂数据准备（SQL/数据库关键词检测）")
    private BigDecimal csFactorC4;
    /**
    * C5：操作步骤数
    */
    @Schema(description = "C5：操作步骤数")
    private BigDecimal csFactorC5;
    /**
    * C6：验证点数
    */
    @Schema(description = "C6：验证点数")
    private BigDecimal csFactorC6;
    /**
    * C7：逻辑分支数量
    */
    @Schema(description = "C7：逻辑分支数量")
    private BigDecimal csFactorC7;
    /**
    * 周期内完成用例数量
    */
    @Schema(description = "周期内完成用例数量")
    private Integer completedCaseCount;
    /**
    * 用例产出率（完成数/总CS）
    */
    @Schema(description = "用例产出率（完成数/总CS）")
    private BigDecimal caseOutputRate;
    /**
    * 被修改用例数量
    */
    @Schema(description = "被修改用例数量")
    private Integer modifiedCaseCount;
    /**
    * 用例变更热度（修改次数总和）
    */
    @Schema(description = "用例变更热度（修改次数总和）")
    private Integer changeHeat;
    /**
    * 高频用例数量（执行次数>=阈值T）
    */
    @Schema(description = "高频用例数量（执行次数>=阈值T）")
    private Integer highFreqCaseCount;
    /**
    * 高频用例执行CS总分
    */
    @Schema(description = "高频用例执行CS总分")
    private BigDecimal highFreqExecCsTotal;
    /**
    * 所有用例执行CS总分
    */
    @Schema(description = "所有用例执行CS总分")
    private BigDecimal allExecCsTotal;
    /**
    * 高价值用例执行热度（%）
    */
    @Schema(description = "高价值用例执行热度（%）")
    private BigDecimal highValueExecRate;
    /**
    * 被复用用例数量
    */
    @Schema(description = "被复用用例数量")
    private Integer reusedCaseCount;
    /**
    * 被复用用例的CS节约分值
    */
    @Schema(description = "被复用用例的CS节约分值")
    private BigDecimal reusedCsSaved;
    /**
    * 测试工时节约率（%）
    */
    @Schema(description = "测试工时节约率（%）")
    private BigDecimal timeSavingRate;
    /**
    * 计划总用例数量
    */
    @Schema(description = "计划总用例数量")
    private Integer planTotalCaseCount;
    /**
    * 计划被复用用例数量（复制+引用）
    */
    @Schema(description = "计划被复用用例数量（复制+引用）")
    private Integer planReusedCaseCount;
    /**
    * 计划直接引用用例数量
    */
    @Schema(description = "计划直接引用用例数量")
    private Integer planDirectRefCount;
    /**
    * 计划独立新增用例数量
    */
    @Schema(description = "计划独立新增用例数量")
    private Integer planNewCaseCount;
    /**
    * 测试计划用例复用率（%）
    */
    @Schema(description = "测试计划用例复用率（%）")
    private BigDecimal planCaseReuseRate;
    /**
    * 计划被修改用例数量
    */
    @Schema(description = "计划被修改用例数量")
    private Integer planModifiedCaseCount;
    /**
    * 测试计划用例修改率（%）
    */
    @Schema(description = "测试计划用例修改率（%）")
    private BigDecimal planCaseModifyRate;
    /**
    * 测试计划用例新增率（%）
    */
    @Schema(description = "测试计划用例新增率（%）")
    private BigDecimal planCaseNewRate;
    /**
    * 计划总执行时长（毫秒）
    */
    @Schema(description = "计划总执行时长（毫秒）")
    private Long planTotalExecDuration;
    /**
    * 计划执行用例数量
    */
    @Schema(description = "计划执行用例数量")
    private Integer planExecCaseCount;
    /**
    * 计划平均用例执行时长（毫秒）
    */
    @Schema(description = "计划平均用例执行时长（毫秒）")
    private Long planAvgExecDuration;
    /**
    * 总执行用例数
    */
    @Schema(description = "总执行用例数")
    private Integer planTotalExecCount;
    /**
    * 最终通过用例数
    */
    @Schema(description = "最终通过用例数")
    private Integer planPassCount;
    /**
    * 测试计划用例通过率（%）
    */
    @Schema(description = "测试计划用例通过率（%）")
    private BigDecimal planPassRate;
    /**
    * 首次执行用例数
    */
    @Schema(description = "首次执行用例数")
    private Integer planFirstExecCount;
    /**
    * 首次执行即通过用例数
    */
    @Schema(description = "首次执行即通过用例数")
    private Integer planFirstPassCount;
    /**
    * 测试计划首次通过率（%）
    */
    @Schema(description = "测试计划首次通过率（%）")
    private BigDecimal planFirstPassRate;
    /**
    * 统计范围内的总用例数
    */
    @Schema(description = "统计范围内的总用例数")
    private Integer totalCaseCount;
    /**
    * 是否已计算：0-未计算, 1-已计算
    */
    @Schema(description = "是否已计算：0-未计算, 1-已计算")
    private Integer isCalculated;
    /**
    * 计算时间（毫秒时间戳）
    */
    @Schema(description = "计算时间（毫秒时间戳）")
    private Long calculationTime;
    /**
    * 创建时间
    */
    @Schema(description = "创建时间")
    private Long createTime;
    /**
    * 更新时间
    */
    @Schema(description = "更新时间")
    private Long updateTime;
}
