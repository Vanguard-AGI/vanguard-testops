package io.vanguard.testops.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 工作流执行记录 VO（用于前端展示）
 */
@Data
public class WorkflowExecutionVO {
    
    @Schema(description = "执行记录ID（用于兼容前端）")
    private String id;
    
    @Schema(description = "运行ID")
    private String runId;
    
    @Schema(description = "报告ID")
    private String reportId;
    
    @Schema(description = "工作流ID")
    private String workflowId;
    
    @Schema(description = "工作流名称")
    private String workflowName;
    
    @Schema(description = "状态: success/failed/running/cancelled")
    private String status;
    
    @Schema(description = "开始时间（毫秒）")
    private Long startTime;
    
    @Schema(description = "结束时间（毫秒）")
    private Long endTime;
    
    @Schema(description = "耗时（秒），支持小数")
    private Double duration;
    
    @Schema(description = "总节点数")
    private Integer totalNodes;
    
    @Schema(description = "成功节点数")
    private Integer successNodes;
    
    @Schema(description = "失败节点数")
    private Integer failedNodes;
    
    @Schema(description = "跳过节点数")
    private Integer skippedNodes;
    
    @Schema(description = "待执行节点数")
    private Integer pendingNodes;
    
    @Schema(description = "执行人")
    private String executor;
    
    @Schema(description = "环境ID")
    private String environmentId;
    
    @Schema(description = "环境名称")
    private String environmentName;
}

