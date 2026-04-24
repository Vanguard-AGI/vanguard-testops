package io.vanguard.testops.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 测试报告列表项 VO
 */
@Data
public class TestReportVO {
    
    @Schema(description = "报告ID")
    private String reportId;

    @Schema(description = "项目ID")
    private String projectId;
    
    @Schema(description = "报告名称")
    private String reportName;
    
    @Schema(description = "报告类型: MANUAL(手动生成)/AUTO(自动生成)/SCHEDULE(定时生成)")
    private String reportType;
    
    @Schema(description = "标签列表")
    private List<String> tags;
    
    @Schema(description = "执行人")
    private String executor;
    
    @Schema(description = "触发类型")
    private String triggerType;
    
    @Schema(description = "报告状态: RUNNING(运行中)/COMPLETED(已完成)/FAILED(失败)/CANCELLED(已取消)")
    private String status;
    
    @Schema(description = "开始时间（毫秒）")
    private Long startTime;
    
    @Schema(description = "结束时间（毫秒）")
    private Long endTime;
    
    @Schema(description = "报告生成耗时（毫秒）：从reportId生成到所有workflow完成的时间差")
    private Long durationMs;
    
    @Schema(description = "执行时长（毫秒）：所有workflow执行耗时的总和")
    private Long executionDurationMs;
    
    @Schema(description = "包含的工作流数量")
    private Integer totalWorkflows;
    
    @Schema(description = "总测试数")
    private Integer totalTests;
    
    @Schema(description = "成功测试数（节点数）")
    private Integer successTests;

    @Schema(description = "成功的工作流个数")
    private Integer successWorkflows;

    @Schema(description = "失败测试数（节点数）")
    private Integer failedTests;

    @Schema(description = "失败的工作流个数")
    private Integer failedWorkflows;
    
    @Schema(description = "跳过测试数")
    private Integer skippedTests;
    
    @Schema(description = "待执行测试数")
    private Integer pendingTests;
    
    @Schema(description = "成功率（百分比）")
    private BigDecimal successRate;
    
    @Schema(description = "平均执行时长（秒）")
    private Integer avgDurationSeconds;
    
    @Schema(description = "报告摘要")
    private String summary;
    
    @Schema(description = "环境ID")
    private String environmentId;
    
    @Schema(description = "环境名称")
    private String environmentName;
    
    @Schema(description = "创建时间（毫秒）")
    private Long createTime;
    
    @Schema(description = "更新时间（毫秒）")
    private Long updateTime;
}

