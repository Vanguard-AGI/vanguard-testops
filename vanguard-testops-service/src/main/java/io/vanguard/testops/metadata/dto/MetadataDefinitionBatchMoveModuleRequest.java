package io.vanguard.testops.metadata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class MetadataDefinitionBatchMoveModuleRequest {

    @Schema(description = "元数据ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "{metadata_definition.ids.not_empty}")
    private List<String> ids;

    @Schema(description = "目标模块ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{metadata_definition.module_id.not_blank}")
    @Size(min = 1, max = 50, message = "{metadata_definition.module_id.length_range}")
    private String moduleId;
}
