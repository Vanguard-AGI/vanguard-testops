package io.vanguard.testops.dashboard.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jan
 */

@Data
public class DashboardViewApiCaseTableRequest extends DashboardViewTableRequest {

	@Schema(description = "接口协议", requiredMode = Schema.RequiredMode.REQUIRED)
	private List<String> protocols = new ArrayList<>();
}
