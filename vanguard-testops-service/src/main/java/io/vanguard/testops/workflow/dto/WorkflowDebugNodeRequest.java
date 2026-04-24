package io.vanguard.testops.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 调试节点请求 DTO
 */
@Data
public class WorkflowDebugNodeRequest {

    @Schema(description = "工作流ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "工作流ID不能为空")
    @Size(max = 50, message = "工作流ID长度不能超过50")
    private String workflowId;

    @Schema(description = "节点ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "节点ID不能为空")
    @Size(max = 50, message = "节点ID长度不能超过50")
    private String nodeId;

    @Schema(description = "节点配置")
    private Object nodeConfig;

    @Schema(description = "用户输入的变量（从环境选择弹窗中获取，如 x-tag-header、x-site-tenant、x-tenant-id、x-app）")
    private Map<String, String> userVariables;
}

