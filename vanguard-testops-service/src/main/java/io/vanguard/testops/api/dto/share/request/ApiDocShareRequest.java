package io.vanguard.testops.api.dto.share.request;

import io.vanguard.testops.api.dto.definition.ApiDefinitionDocRequest;
import io.vanguard.testops.sdk.domain.ShareInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author: Jan
 * @date: 2026-04-22
 * @version: 1.0
 */
@Data
public class ApiDocShareRequest extends ShareInfo {

    @Schema(description = "接口pk")
    @Size(min = 1, max = 50, message = "{api_definition.id.length_range}")
    private String apiId;

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{api_definition.project_id.not_blank}")
    @Size(min = 1, max = 50, message = "{api_definition.project_id.length_range}")
    private String projectId;

    @Schema(description = "接口文档分享选择")
    private ApiDefinitionDocRequest selectRequest;
}
