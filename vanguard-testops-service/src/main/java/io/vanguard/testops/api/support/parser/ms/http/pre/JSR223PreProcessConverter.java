package io.vanguard.testops.api.support.parser.ms.http.pre;

import io.vanguard.testops.api.support.parser.ms.ConverterUtils;
import io.vanguard.testops.plugin.api.spi.AbstractMsElementConverter;
import io.vanguard.testops.plugin.api.spi.AbstractMsTestElement;
import io.vanguard.testops.project.api.processor.ScriptProcessor;
import jodd.util.StringUtil;
import org.apache.jmeter.modifiers.JSR223PreProcessor;
import org.apache.jorphan.collections.HashTree;

public class JSR223PreProcessConverter extends AbstractMsElementConverter<JSR223PreProcessor> {
    @Override
    public void toMsElement(AbstractMsTestElement parent, JSR223PreProcessor element, HashTree hashTree) {
        ScriptProcessor msScriptElement = new ScriptProcessor();
        String scriptLanguage = element.getScriptLanguage();
        if (StringUtil.isBlank(scriptLanguage)) {
            scriptLanguage = element.getPropertyAsString("scriptLanguage");
        }
        msScriptElement.setScriptLanguage(ConverterUtils.parseScriptLanguage(scriptLanguage));
        msScriptElement.setEnable(element.isEnabled());
        msScriptElement.setName(element.getPropertyAsString("TestElement.name"));
        msScriptElement.setScript(element.getPropertyAsString("script"));

        ConverterUtils.addPreProcess(parent, msScriptElement);
    }
}
