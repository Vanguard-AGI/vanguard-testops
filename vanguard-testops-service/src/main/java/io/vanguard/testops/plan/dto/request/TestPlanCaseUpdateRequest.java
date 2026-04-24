package io.vanguard.testops.plan.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


/**
 * @author Jan
 */
@Data
public class TestPlanCaseUpdateRequest extends BasePlanCaseBatchRequest {

    @Schema(description = "执行人id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{test_plan.user_id.not_blank}")
    private String userId;
}
