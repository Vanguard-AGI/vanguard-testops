package io.vanguard.testops.metadata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MetadataModuleCreateRequest {

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{project.id.not_blank}")
    @Size(min = 1, max = 50, message = "{metadata_module.project_id.length_range}")
    private String projectId;

    @Schema(description = "模块名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{metadata_module.name.not_blank}")
    @Pattern(regexp = "^[^\\\\/]*$", message = "{metadata_module.name.not_contain_slash}")
    @Size(min = 1, max = 255, message = "{metadata_module.name.length_range}")
    private String name;

    @Schema(description = "父模块ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{parent.node.not_blank}")
    @Size(min = 1, max = 50, message = "{metadata_module.parent_id.length_range}")
    private String parentId = "ROOT";

    @Schema(description = "目录类型: API/WORKFLOW/SQL/MIXED", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{metadata_module.module_type.not_blank}")
    @Size(min = 1, max = 20, message = "{metadata_module.module_type.length_range}")
    private String moduleType = "MIXED";

    @Schema(description = "类型ID（根据module_type来存对应业务下第一层ID，如WORKFLOW类型存储workspace_id）")
    @Size(max = 50, message = "type_id长度不能超过50")
    private String typeId;
}
