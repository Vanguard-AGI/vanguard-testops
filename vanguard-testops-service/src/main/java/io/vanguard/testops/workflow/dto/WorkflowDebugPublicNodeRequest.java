package io.vanguard.testops.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 调试公共节点请求 DTO（无工作流上下文，仅按节点配置执行）
 */
@Data
public class WorkflowDebugPublicNodeRequest {

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "项目ID不能为空")
    @Size(max = 50, message = "项目ID长度不能超过50")
    private String projectId;

    @Schema(description = "节点ID（公共节点ID）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "节点ID不能为空")
    @Size(max = 50, message = "节点ID长度不能超过50")
    private String nodeId;

    @Schema(description = "节点配置（含 environmentId、method、url 等）")
    private Object nodeConfig;

    @Schema(description = "节点类型，如 HTTP_REQUEST、MYSQL，用于执行机转换；不传则从 nodeConfig 推断")
    @Size(max = 32)
    private String nodeType;

    @Schema(description = "节点名称，用于运行记录展示；不传则用「公共节点」")
    @Size(max = 255)
    private String nodeName;

    @Schema(description = "用户输入的变量（如 x-tag-header、x-site-tenant、x-tenant-id、x-app）")
    private Map<String, String> userVariables;
}
