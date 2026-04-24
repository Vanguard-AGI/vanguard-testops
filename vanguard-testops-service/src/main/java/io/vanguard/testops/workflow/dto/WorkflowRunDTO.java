package io.vanguard.testops.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 工作流运行详情 DTO
 */
@Data
public class WorkflowRunDTO {
    
    @Schema(description = "运行ID")
    private String runId;
    
    @Schema(description = "工作流ID")
    private String workflowId;
    
    @Schema(description = "项目ID")
    private String projectId;
    
    @Schema(description = "工作流名称")
    private String workflowName;
    
    @Schema(description = "触发类型")
    private String triggerType;
    
    @Schema(description = "触发人")
    private String triggerUser;
    
    @Schema(description = "状态")
    private String status;
    
    @Schema(description = "开始时间")
    private Long startTime;
    
    @Schema(description = "结束时间")
    private Long endTime;
    
    @Schema(description = "总耗时（毫秒）")
    private Long durationMs;
    
    @Schema(description = "总步骤数")
    private Integer totalSteps;
    
    @Schema(description = "通过步骤数")
    private Integer passedCount;
    
    @Schema(description = "失败步骤数")
    private Integer failedCount;
    
    @Schema(description = "跳过步骤数")
    private Integer skippedCount;
    
    @Schema(description = "待执行步骤数")
    private Integer pendingCount;
    
    @Schema(description = "运行结果摘要")
    private Map<String, Object> resultSummary;
    
    @Schema(description = "环境ID")
    private String environmentId;
    
    @Schema(description = "环境名称")
    private String environmentName;
    
    @Schema(description = "步骤执行明细列表")
    private List<WorkflowRunStepDTO> steps;
}

