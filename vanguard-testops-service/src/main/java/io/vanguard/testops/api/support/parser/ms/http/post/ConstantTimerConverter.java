package io.vanguard.testops.api.support.parser.ms.http.post;

import io.vanguard.testops.api.support.parser.ms.ConverterUtils;
import io.vanguard.testops.plugin.api.spi.AbstractMsElementConverter;
import io.vanguard.testops.plugin.api.spi.AbstractMsTestElement;
import io.vanguard.testops.project.api.processor.TimeWaitingProcessor;
import org.apache.jmeter.timers.ConstantTimer;
import org.apache.jorphan.collections.HashTree;

public class ConstantTimerConverter extends AbstractMsElementConverter<ConstantTimer> {
    @Override
    public void toMsElement(AbstractMsTestElement parent, ConstantTimer element, HashTree hashTree) {
        TimeWaitingProcessor msProcessor = new TimeWaitingProcessor();
        msProcessor.setDelay(Long.parseLong(element.getDelay()));
        msProcessor.setEnable(element.isEnabled());
        msProcessor.setName(element.getName());
        ConverterUtils.addPreProcess(parent, msProcessor);
    }
}
