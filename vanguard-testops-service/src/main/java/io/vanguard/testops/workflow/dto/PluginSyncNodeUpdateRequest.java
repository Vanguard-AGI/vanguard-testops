package io.vanguard.testops.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "插件同步节点更新请求")
public class PluginSyncNodeUpdateRequest {

    @Schema(description = "节点ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "节点ID不能为空")
    @Size(min = 1, max = 50, message = "节点ID长度范围1-50")
    private String nodeId;

    @Schema(description = "节点数据（JSON格式，包含endpoint的完整信息）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "节点数据不能为空")
    private Map<String, Object> endpointData;
}


