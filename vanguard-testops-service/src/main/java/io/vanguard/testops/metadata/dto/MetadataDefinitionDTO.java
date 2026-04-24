package io.vanguard.testops.metadata.dto;

import io.vanguard.testops.metadata.domain.MetadataDefinition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class MetadataDefinitionDTO extends MetadataDefinition {

    @Schema(description = "请求配置(JSON字符串，用于前端展示)")
    private String requestConfigJson;

    @Schema(description = "响应配置(JSON字符串，用于前端展示)")
    private String responseConfigJson;
}
