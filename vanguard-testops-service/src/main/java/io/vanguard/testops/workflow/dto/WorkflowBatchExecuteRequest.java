package io.vanguard.testops.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 工作流批量执行请求 DTO
 */
@Data
@Schema(description = "工作流批量执行请求")
public class WorkflowBatchExecuteRequest {

    @Schema(description = "工作流ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "工作流ID列表不能为空")
    private List<@Size(max = 50, message = "工作流ID长度不能超过50") String> workflowIds;

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 50, message = "项目ID长度不能超过50")
    private String projectId;

    @Schema(description = "报告名称（可选）")
    @Size(max = 255, message = "报告名称长度不能超过255")
    private String reportName;

    @Schema(description = "执行环境ID")
    @Size(max = 50, message = "环境ID长度不能超过50")
    private String environmentId;

    @Schema(description = "执行环境名称")
    @Size(max = 255, message = "环境名称长度不能超过255")
    private String environmentName;

    @Schema(description = "触发类型: MANUAL(手动)/SCHEDULE(定时)/API(接口触发)")
    @Size(max = 20, message = "触发类型长度不能超过20")
    private String triggerType = "MANUAL";

    @Schema(description = "用户输入的变量（批量执行时使用，优先级高于环境变量）")
    private Map<String, String> userVariables;
}

