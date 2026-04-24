package io.vanguard.testops.dashboard.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author Jan
 */

@Data
public class DashboardViewPlanTableRequest extends DashboardViewTableRequest {

	@Schema(description = "类型", allowableValues = {"ALL", "TEST_PLAN", "GROUP"}, requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "{test_plan.type.not_blank}")
	private String type;
}
