package io.vanguard.testops.api.dto.export;

import io.vanguard.testops.api.domain.ApiScenarioStep;
import io.vanguard.testops.plugin.api.spi.AbstractMsTestElement;
import lombok.Data;

@Data
public class ApiScenarioStepExportDTO extends ApiScenarioStep {
    private AbstractMsTestElement stepComponent;
}
