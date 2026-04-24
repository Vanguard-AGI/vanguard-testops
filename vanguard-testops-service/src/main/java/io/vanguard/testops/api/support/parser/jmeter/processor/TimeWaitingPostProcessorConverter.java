package io.vanguard.testops.api.support.parser.jmeter.processor;

import io.vanguard.testops.plugin.api.dto.ParameterConfig;
import io.vanguard.testops.project.api.processor.ScriptProcessor;
import io.vanguard.testops.project.api.processor.TimeWaitingProcessor;
import io.vanguard.testops.project.constants.ScriptLanguageType;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.extractor.JSR223PostProcessor;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jorphan.collections.HashTree;


/**
 * @Author: Jan
 * @CreateTime: 2023-12-26  14:49
 */
public class TimeWaitingPostProcessorConverter extends MsProcessorConverter<TimeWaitingProcessor> {
    @Override
    public void parse(HashTree hashTree, TimeWaitingProcessor timeWaitingProcessor, ParameterConfig config) {
        if (!needParse(timeWaitingProcessor, config)) {
            return;
        }
        TestElement processor = new JSR223PostProcessor();

        ScriptProcessor scriptProcessor = new ScriptProcessor();
        String name = StringUtils.isBlank(timeWaitingProcessor.getName()) ?
                (timeWaitingProcessor.getDelay() + " ms") : timeWaitingProcessor.getName();
        scriptProcessor.setName(name);
        scriptProcessor.setScriptLanguage(ScriptLanguageType.BEANSHELL_JSR233.name());
        scriptProcessor.setScript(String.format("try {\n"
                + "    Thread.sleep(%s);\n"
                + "} catch (InterruptedException e) {\n"
                + "    e.printStackTrace();\n"
                + "}\n", timeWaitingProcessor.getDelay()));
        ScriptProcessorConverter.parse(processor, scriptProcessor, config);

        hashTree.add(processor);
    }

    protected boolean needParse(TimeWaitingProcessor timeWaitingProcessor, ParameterConfig config) {
        if (timeWaitingProcessor.getDelay() != null && timeWaitingProcessor.getDelay() > 0) {
            return true;
        }
        return super.needParse(timeWaitingProcessor, config);
    }
}
