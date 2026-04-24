package io.vanguard.testops.plan.dto.request;

import io.vanguard.testops.plan.constants.TreeTypeEnums;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author Jan
 */
@Data
public class TestPlanApiCaseModuleRequest extends TestPlanApiCaseRequest{

    @Schema(description = "类型：模块/计划集", allowableValues = {"MODULE","COLLECTION"},requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{test_plan.type.not_blank}")
    private String treeType = TreeTypeEnums.COLLECTION;
}
