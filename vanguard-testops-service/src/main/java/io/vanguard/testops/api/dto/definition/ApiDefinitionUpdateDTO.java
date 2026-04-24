package io.vanguard.testops.api.dto.definition;

import io.vanguard.testops.api.domain.ApiDefinition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Jan
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ApiDefinitionUpdateDTO extends ApiDefinition {

    @Schema(description = "用例数")
    private int caseTotal;

    @Schema(description = "场景数")
    private int scenarioTotal;
}
