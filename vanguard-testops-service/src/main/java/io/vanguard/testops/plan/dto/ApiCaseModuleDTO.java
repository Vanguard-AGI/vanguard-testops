package io.vanguard.testops.plan.dto;

import io.vanguard.testops.api.domain.ApiDefinitionModule;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author Jan
 */
@Data
public class ApiCaseModuleDTO extends ApiDefinitionModule {

    @Schema(description = "项目名称")
    private String projectName;
}
