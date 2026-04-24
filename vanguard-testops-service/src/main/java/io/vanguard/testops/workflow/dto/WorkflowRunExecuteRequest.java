package io.vanguard.testops.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 工作流执行请求 DTO
 */
@Data
public class WorkflowRunExecuteRequest {

    @Schema(description = "工作流ID（单个执行时使用）")
    @Size(max = 50, message = "工作流ID长度不能超过50")
    private String workflowId;

    @Schema(description = "工作流ID列表（批量执行时使用）")
    private List<@Size(max = 50, message = "工作流ID长度不能超过50") String> workflowIds;

    @Schema(description = "执行环境ID")
    @Size(max = 50, message = "环境ID长度不能超过50")
    private String environmentId;

    @Schema(description = "触发类型: MANUAL/SCHEDULE/API", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "触发类型不能为空")
    @Size(max = 20, message = "触发类型长度不能超过20")
    private String triggerType = "MANUAL";

    @Schema(description = "用户输入的变量（批量执行时使用，优先级高于环境变量）")
    private Map<String, String> userVariables;
}

