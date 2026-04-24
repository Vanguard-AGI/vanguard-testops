package io.vanguard.testops.metadata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 脚本执行请求：根据 definition_id 拉取脚本与 request_config，组装参数后转发到 Aegis 执行。
 * params 为 key:value 形式，key 与 request_config.userParams 中的 paramName 对应。
 */
@Data
public class ScriptRunRequest {

    @Schema(description = "元数据定义ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{metadata_definition.definition_id.not_blank}")
    private String definitionId;

    @Schema(description = "参数，key:value 形式，如 {\"env\": \"env11111111\"}")
    private Map<String, String> params;

    @Schema(description = "执行次数", example = "1")
    private Integer executionCount = 1;

    @Schema(description = "环境ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "environmentId 不能为空")
    private String environmentId;
}
