package io.vanguard.testops.api.support.parser.ms.http.post;

import io.vanguard.testops.api.support.parser.ms.ConverterUtils;
import io.vanguard.testops.plugin.api.spi.AbstractMsElementConverter;
import io.vanguard.testops.plugin.api.spi.AbstractMsTestElement;
import io.vanguard.testops.project.api.processor.ScriptProcessor;
import io.vanguard.testops.project.constants.ScriptLanguageType;
import org.apache.jmeter.extractor.BeanShellPostProcessor;
import org.apache.jorphan.collections.HashTree;

public class BeanShellPostProcessConverter extends AbstractMsElementConverter<BeanShellPostProcessor> {
    @Override
    public void toMsElement(AbstractMsTestElement parent, BeanShellPostProcessor element, HashTree hashTree) {
        ScriptProcessor msScriptElement = new ScriptProcessor();
        msScriptElement.setEnable(element.isEnabled());
        msScriptElement.setScriptLanguage(ScriptLanguageType.BEANSHELL.name());
        msScriptElement.setName(element.getPropertyAsString("TestElement.name"));
        msScriptElement.setScript(element.getPropertyAsString("script"));
        ConverterUtils.addPostProcess(parent, msScriptElement);
    }
}
