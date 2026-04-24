package io.vanguard.testops.api.support.parser.jmeter.controller;

import io.vanguard.testops.api.dto.request.controller.MsConstantTimerController;
import io.vanguard.testops.api.support.parser.jmeter.processor.ScenarioTimeWaitingProcessorConverter;
import io.vanguard.testops.plugin.api.dto.ParameterConfig;
import io.vanguard.testops.plugin.api.spi.AbstractJmeterElementConverter;
import io.vanguard.testops.project.api.processor.TimeWaitingProcessor;
import io.vanguard.testops.sdk.util.BeanUtils;
import io.vanguard.testops.sdk.util.LogUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.jorphan.collections.HashTree;

public class MsConstantTimerControllerConverter extends AbstractJmeterElementConverter<MsConstantTimerController> {

    @Override
    public void toHashTree(HashTree tree, MsConstantTimerController element, ParameterConfig config) {
        if (BooleanUtils.isFalse(element.getEnable())) {
            LogUtils.info("MsConstantTimerController is disabled");
            return;
        }
        TimeWaitingProcessor timeWaitingProcessor = BeanUtils.copyBean(new TimeWaitingProcessor(), element);
        new ScenarioTimeWaitingProcessorConverter().parse(tree, timeWaitingProcessor, config);
    }
}
