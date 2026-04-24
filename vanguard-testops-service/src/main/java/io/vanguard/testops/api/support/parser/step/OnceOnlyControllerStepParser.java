package io.vanguard.testops.api.support.parser.step;

import io.vanguard.testops.api.dto.request.controller.MsOnceOnlyController;
import io.vanguard.testops.api.dto.scenario.ApiScenarioStepCommonDTO;
import io.vanguard.testops.api.dto.scenario.ApiScenarioStepDetailRequest;
import io.vanguard.testops.plugin.api.spi.AbstractMsTestElement;
import io.vanguard.testops.sdk.util.BeanUtils;

public class OnceOnlyControllerStepParser extends StepParser {
    @Override
    public AbstractMsTestElement parseTestElement(ApiScenarioStepCommonDTO step, String resourceBlob, String stepDetail) {
        MsOnceOnlyController msOnceOnlyController = new MsOnceOnlyController();
        BeanUtils.copyBean(msOnceOnlyController, step);
        return msOnceOnlyController;
    }

    @Override
    public Object parseDetail(ApiScenarioStepDetailRequest step) {
        return null;
    }
}
