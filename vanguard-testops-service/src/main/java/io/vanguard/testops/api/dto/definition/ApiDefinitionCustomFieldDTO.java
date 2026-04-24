package io.vanguard.testops.api.dto.definition;

import io.vanguard.testops.system.domain.CustomField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author: Jan
 * @date: 2026-04-22
 * @version: 1.0
 */
@Data
public class ApiDefinitionCustomFieldDTO extends CustomField {
    @Schema(description = "字段值")
    private String value;

    @Schema(description = "接口ID")
    private String apiId;
}
