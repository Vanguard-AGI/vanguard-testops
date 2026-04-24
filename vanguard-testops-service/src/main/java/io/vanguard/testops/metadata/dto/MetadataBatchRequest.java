package io.vanguard.testops.metadata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class MetadataBatchRequest {

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String projectId;

    @Schema(description = "选中的ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "{metadata.ids.not_empty}")
    private List<String> ids;

    @Schema(description = "目标模块ID")
    private String targetModuleId;
}
