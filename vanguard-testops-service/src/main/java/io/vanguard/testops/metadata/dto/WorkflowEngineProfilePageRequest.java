package io.vanguard.testops.metadata.dto;

import io.vanguard.testops.system.dto.sdk.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class WorkflowEngineProfilePageRequest extends BasePageRequest {

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{workflow_engine_profile.project_id.not_blank}")
    @Size(min = 1, max = 50, message = "{workflow_engine_profile.project_id.length_range}")
    private String projectId;

    @Schema(description = "引擎类型: API/UI")
    private String engineType;

    @Schema(description = "环境: DEV/TEST/PROD")
    private String envCode;

    @Schema(description = "搜索关键字")
    private String keyword;
}

