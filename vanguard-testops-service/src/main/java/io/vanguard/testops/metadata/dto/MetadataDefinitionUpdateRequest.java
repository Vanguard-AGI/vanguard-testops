package io.vanguard.testops.metadata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class MetadataDefinitionUpdateRequest {

    @Schema(description = "元数据ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{metadata_definition.id.not_blank}")
    @Size(min = 1, max = 50, message = "{metadata_definition.id.length_range}")
    private String id;

    @Schema(description = "名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{metadata_definition.name.not_blank}")
    @Size(min = 1, max = 255, message = "{metadata_definition.name.length_range}")
    private String name;

    @Schema(description = "模块ID")
    @Size(min = 1, max = 50, message = "{metadata_definition.module_id.length_range}")
    private String moduleId;

    @Schema(description = "描述")
    @Size(max = 1000, message = "{metadata_definition.description.length_range}")
    private String description;

    @Schema(description = "标签")
    private List<String> tags;

    @Schema(description = "请求配置(URL/Method/Headers/Body)")
    private Map<String, Object> requestConfig;

    @Schema(description = "响应配置(Schema/Extract)")
    private Map<String, Object> responseConfig;

    @Schema(description = "SQL语句 / Python脚本 / Shell脚本")
    private String scriptContent;

    @Schema(description = "是否为案例：0-否，1-是")
    private Boolean isCase;
}
