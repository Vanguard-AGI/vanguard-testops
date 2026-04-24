package io.vanguard.testops.api.support.parser.step;

import io.vanguard.testops.api.dto.request.controller.MsLoopController;
import io.vanguard.testops.api.dto.scenario.ApiScenarioStepCommonDTO;
import io.vanguard.testops.api.dto.scenario.ApiScenarioStepDetailRequest;
import io.vanguard.testops.plugin.api.spi.AbstractMsTestElement;

public class LoopControllerStepParser extends StepParser {

    @Override
    public AbstractMsTestElement parseTestElement(ApiScenarioStepCommonDTO step, String resourceBlob, String stepDetail) {
        return parseConfig2TestElement(step, MsLoopController.class);
    }

    @Override
    public Object parseDetail(ApiScenarioStepDetailRequest step) {
        return null;
    }
}
