package io.vanguard.testops.api.support.parser.step;

import io.vanguard.testops.api.dto.request.MsJMeterComponent;
import io.vanguard.testops.api.dto.scenario.ApiScenarioStepCommonDTO;
import io.vanguard.testops.api.dto.scenario.ApiScenarioStepDetailRequest;
import io.vanguard.testops.api.support.data.ApiDataUtils;
import io.vanguard.testops.plugin.api.spi.AbstractMsTestElement;

public class JMeterComponentStepParser extends StepParser {
    @Override
    public AbstractMsTestElement parseTestElement(ApiScenarioStepCommonDTO step, String resourceBlob, String stepDetail) {
        return ApiDataUtils.parseObject(stepDetail, MsJMeterComponent.class);
    }

    @Override
    public Object parseDetail(ApiScenarioStepDetailRequest step) {
        return null;
    }
}
