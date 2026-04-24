package io.vanguard.testops.metadata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MetadataModuleUpdateRequest {

    @Schema(description = "模块ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{metadata_module.id.not_blank}")
    @Size(min = 1, max = 50, message = "{metadata_module.id.length_range}")
    private String id;

    @Schema(description = "模块名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{metadata_module.name.not_blank}")
    @Pattern(regexp = "^[^\\\\/]*$", message = "{metadata_module.name.not_contain_slash}")
    @Size(min = 1, max = 255, message = "{metadata_module.name.length_range}")
    private String name;
}
