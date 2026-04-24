package io.vanguard.testops.api.support.parser.jmeter.processor.assertion;

import io.vanguard.testops.api.support.parser.jmeter.constants.JmeterAlias;
import io.vanguard.testops.api.support.parser.jmeter.constants.JmeterProperty;
import io.vanguard.testops.api.support.parser.jmeter.processor.ScriptProcessorConverter;
import io.vanguard.testops.plugin.api.dto.ParameterConfig;
import io.vanguard.testops.project.api.assertion.MsScriptAssertion;
import io.vanguard.testops.project.api.processor.ScriptProcessor;
import io.vanguard.testops.project.constants.ScriptLanguageType;
import io.vanguard.testops.sdk.dto.api.result.ResponseAssertionResult;
import io.vanguard.testops.sdk.util.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.assertions.BeanShellAssertion;
import org.apache.jmeter.assertions.JSR223Assertion;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jorphan.collections.HashTree;

import java.util.Optional;

/**
 * @Author: Jan
 * @CreateTime: 2023-12-27  21:01
 */
public class ScriptAssertionConverter extends AssertionConverter<MsScriptAssertion> {
    @Override
    public void parse(HashTree hashTree, MsScriptAssertion msAssertion, ParameterConfig config, boolean isIgnoreStatus) {
        if (!needParse(msAssertion, config) || !msAssertion.isValid()) {
            return;
        }

        AbstractTestElement assertion;
        if (isJSR233(msAssertion)) {
            assertion = new JSR223Assertion();
        } else {
            assertion = new BeanShellAssertion();
        }
        ScriptProcessor scriptProcessor = BeanUtils.copyBean(new ScriptProcessor(), msAssertion);
        ScriptProcessorConverter.parse(assertion, scriptProcessor, config);

        if (!isJSR233(msAssertion)) {
            // beanshell 断言参数名有区别，替换一下
            assertion.setProperty(JmeterProperty.BEAN_SHELL_ASSERTION_QUERY, assertion.getProperty(JmeterProperty.SCRIPT).getStringValue());
            assertion.setProperty(TestElement.GUI_CLASS, SaveService.aliasToClass(JmeterAlias.BEAN_SHELL_ASSERTION_GUI));
        }

        // 添加公共脚本的参数
        Optional.ofNullable(ScriptProcessorConverter.getScriptArguments(scriptProcessor))
                .ifPresent(hashTree::add);

        setMsAssertionInfoProperty(assertion, ResponseAssertionResult.AssertionResultType.SCRIPT.name(), assertion.getName());

        hashTree.add(assertion);
    }

    public static boolean isJSR233(MsScriptAssertion msScriptAssertion) {
        return !StringUtils.equals(msScriptAssertion.getScriptLanguage(), ScriptLanguageType.BEANSHELL.name());
    }
}
