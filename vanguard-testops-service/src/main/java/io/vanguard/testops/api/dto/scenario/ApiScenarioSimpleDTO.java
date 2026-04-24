package io.vanguard.testops.api.dto.scenario;

import io.vanguard.testops.api.domain.ApiScenario;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ApiScenarioSimpleDTO extends ApiScenario {

    @Schema(description = "更新人名称")
    private String updateUserName;
    @Schema(description = "创建人名称")
    private String createUserName;
}
