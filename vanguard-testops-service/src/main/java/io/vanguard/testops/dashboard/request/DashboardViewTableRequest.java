package io.vanguard.testops.dashboard.request;

import io.vanguard.testops.system.dto.sdk.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author Jan
 */

@Data
public class DashboardViewTableRequest extends BasePageRequest {

	@Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "{project.id.not_blank}")
	private String projectId;
}
