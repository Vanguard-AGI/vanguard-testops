package io.vanguard.testops.workflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 工作流步骤执行明细 DTO
 */
@Data
public class WorkflowRunStepDTO {
    
    @Schema(description = "运行步骤ID")
    private String runStepId;
    
    @Schema(description = "步骤ID")
    private String stepId;
    
    @Schema(description = "步骤名称")
    private String stepName;
    
    @Schema(description = "步骤类型")
    private String stepType;
    
    @Schema(description = "状态")
    private String status;
    
    @Schema(description = "开始时间")
    private Long startTime;
    
    @Schema(description = "结束时间")
    private Long endTime;
    
    @Schema(description = "耗时（毫秒）")
    private Long durationMs;
    
    @Schema(description = "实际请求内容")
    private Map<String, Object> requestData;
    
    @Schema(description = "实际响应内容")
    private Map<String, Object> responseData;
    
    @Schema(description = "断言验证日志")
    @JsonProperty("assertion")
    private List<Map<String, Object>> assertion;
    
    @Schema(description = "提取的变量")
    private Map<String, Object> extractVars;
    
    @Schema(description = "错误消息")
    private String errorMsg;
    
    @Schema(description = "执行描述")
    private String description;
}

