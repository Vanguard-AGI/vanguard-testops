package io.vanguard.testops.api.support.parser.jmeter.processor.assertion;

import io.vanguard.testops.api.support.parser.jmeter.processor.ScriptProcessorConverter;
import io.vanguard.testops.plugin.api.dto.ParameterConfig;
import io.vanguard.testops.project.api.assertion.MsVariableAssertion;
import io.vanguard.testops.project.api.processor.ScriptProcessor;
import io.vanguard.testops.project.constants.ScriptLanguageType;
import io.vanguard.testops.sdk.constants.MsAssertionCondition;
import io.vanguard.testops.sdk.dto.api.result.ResponseAssertionResult;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.jmeter.assertions.JSR223Assertion;
import org.apache.jorphan.collections.HashTree;

import java.util.HashMap;

/**
 * @Author: Jan
 * @CreateTime: 2023-12-27  21:01
 */
public class VariableAssertionConverter extends AssertionConverter<MsVariableAssertion> {
    @Override
    public void parse(HashTree hashTree, MsVariableAssertion msAssertion, ParameterConfig config, boolean isIgnoreStatus) {
        if (!needParse(msAssertion, config)) {
            return;
        }
        Boolean globalEnable = msAssertion.getEnable();
        msAssertion.getVariableAssertionItems()
                .stream()
                .filter(this::isValid)
                .forEach(variableAssertionItem -> {
                    if (needParse(variableAssertionItem, config)) {
                        JSR223Assertion jsr223Assertion = parse2JSR233Assertion(variableAssertionItem, config);
                        jsr223Assertion.setEnabled(variableAssertionItem.getEnable());
                        if (BooleanUtils.isFalse(globalEnable)) {
                            // 如果整体禁用，则禁用
                            jsr223Assertion.setEnabled(false);
                        }
                        hashTree.add(jsr223Assertion);
                    }
                });
    }

    protected boolean needParse(MsVariableAssertion.VariableAssertionItem variableAssertionItem, ParameterConfig config) {
        // 如果组件是启用的，或者设置了解析禁用的组件，则返回 true
        return BooleanUtils.isTrue(variableAssertionItem.getEnable()) || config.getParseDisabledElement();
    }

    private static JSR223Assertion parse2JSR233Assertion(MsVariableAssertion.VariableAssertionItem variableAssertionItem, ParameterConfig config) {
        ScriptProcessor scriptProcessor = new ScriptProcessor();
        scriptProcessor.setScript(parse2BeanshellJSR233Script(variableAssertionItem));

        String variableName = variableAssertionItem.getVariableName();
        String condition = variableAssertionItem.getCondition();
        String expectedValue = variableAssertionItem.getExpectedValue();
        String name = String.format("Variable '%s' expect %s %s", variableName, condition.toLowerCase().replace("_", ""), expectedValue);
        scriptProcessor.setName(name);

        scriptProcessor.setScriptLanguage(ScriptLanguageType.GROOVY.name());
        JSR223Assertion jsr223Assertion = new JSR223Assertion();
        ScriptProcessorConverter.parse(jsr223Assertion, scriptProcessor, config);

        setMsAssertionInfoProperty(jsr223Assertion, ResponseAssertionResult.AssertionResultType.VARIABLE.name(), variableName, condition, expectedValue);
        return jsr223Assertion;
    }

    public boolean isValid(MsVariableAssertion.VariableAssertionItem variableAssertionItem) {
        return StringUtils.isNotBlank(variableAssertionItem.getVariableName())
                && StringUtils.isNotBlank(variableAssertionItem.getCondition())
                && BooleanUtils.isTrue(variableAssertionItem.getEnable())
                && !StringUtils.equals(variableAssertionItem.getCondition(), MsAssertionCondition.UNCHECK.name());
    }

    private static String parse2BeanshellJSR233Script(MsVariableAssertion.VariableAssertionItem variableAssertionItem) {
        HashMap<String, String> handleMap = new HashMap<>();
        StringBuilder script = new StringBuilder();
        script.append(String.format("variableValue = vars.get(\"%s\");\n",
                StringEscapeUtils.escapeJava(variableAssertionItem.getVariableName())));
        script.append(String.format("expectation = \"%s\";\n",
                StringEscapeUtils.escapeJava(variableAssertionItem.getExpectedValue())));
        script.append("flag = true;\n");

        handleMap.put(MsAssertionCondition.REGEX.name(),
                "import java.util.regex.Pattern;\n"
                        + "if (variableValue != null) {\n"
                        + "   result = Pattern.matches(expectation, variableValue);\n"
                        + "} else {\n"
                        + "   result = false;\n"
                        + "}\n"
                        + "msg = variableValue + \" not matching \" + expectation;\n");
        handleMap.put(MsAssertionCondition.EQUALS.name(), "result = variableValue.equals(expectation);\nmsg = variableValue + \" == \" + expectation;\n");
        handleMap.put(MsAssertionCondition.NOT_EQUALS.name(), "result = !variableValue.equals(expectation);\nmsg = variableValue + \" != \" + expectation;\n");
        handleMap.put(MsAssertionCondition.CONTAINS.name(), "result = variableValue.contains(expectation);\nmsg = variableValue + \" contains \" + expectation;\n");
        handleMap.put(MsAssertionCondition.NOT_CONTAINS.name(), "result = !variableValue.contains(expectation);\nmsg = variableValue + \" not contains \" + expectation;\n");
        handleMap.put(MsAssertionCondition.GT.name(), "result = Double.parseDouble(variableValue) > Double.parseDouble(expectation);\nmsg = variableValue + \" > \" + expectation;\n");
        handleMap.put(MsAssertionCondition.GT_OR_EQUALS.name(), "result = Double.parseDouble(variableValue) >= Double.parseDouble(expectation);\nmsg = variableValue + \" >= \" + expectation;\n");
        handleMap.put(MsAssertionCondition.LT.name(), "result = Double.parseDouble(variableValue) < Double.parseDouble(expectation);\nmsg = variableValue + \" < \" + expectation;\n");
        handleMap.put(MsAssertionCondition.LT_OR_EQUALS.name(), "result = Double.parseDouble(variableValue) <= Double.parseDouble(expectation);\nmsg = variableValue + \" <= \" + expectation;\n");
        handleMap.put(MsAssertionCondition.START_WITH.name(), "result = variableValue.startsWith(expectation);\nmsg = variableValue + \" start with \" + expectation;\n");
        handleMap.put(MsAssertionCondition.END_WITH.name(), "result = variableValue.endsWith(expectation);\nmsg = variableValue + \" end with \" + expectation;\n");
        handleMap.put(MsAssertionCondition.LENGTH_EQUALS.name(), "number = Double.parseDouble(expectation);\nresult = variableValue.length() == number;\nmsg = variableValue + \" length == \" + expectation;\n");
        handleMap.put(MsAssertionCondition.LENGTH_GT.name(), "number = Double.parseDouble(expectation);\nresult = variableValue.length() > number;\nmsg = variableValue + \" length > \" + expectation;\n");
        handleMap.put(MsAssertionCondition.LENGTH_GT_OR_EQUALS.name(), "number = Double.parseDouble(expectation);\nresult = variableValue.length() >= number;\nmsg = variableValue + \" length >= \" + expectation;\n");
        handleMap.put(MsAssertionCondition.LENGTH_LT.name(), "number = Double.parseDouble(expectation);\nresult = variableValue.length() < number;\nmsg = variableValue + \" length < \" + expectation;\n");
        handleMap.put(MsAssertionCondition.LENGTH_LT_OR_EQUALS.name(), "number = Double.parseDouble(expectation);\nresult = variableValue.length() <= number;\nmsg = variableValue + \" length <= \" + expectation;\n");
        handleMap.put(MsAssertionCondition.EMPTY.name(), "result = variableValue == void || variableValue.length() == 0;\nmsg = variableValue + \" is empty\";\nflag = false;\n");
        handleMap.put(MsAssertionCondition.NOT_EMPTY.name(), "result = variableValue != void && variableValue.length() > 0;\nmsg = variableValue + \" is not empty\";\nflag = false;\n");

        String condition = variableAssertionItem.getCondition();
        String handleScript = handleMap.get(condition);
        if (StringUtils.isBlank(handleScript)) {
            script.append(handleMap.get(MsAssertionCondition.EQUALS.name()));
        } else {
            script.append(handleScript);
        }

        script.append("if (!result){\n");
        script.append("    if (flag) {\n");
        script.append("        msg = \"assertion [\" + msg + \"]: false;\";\n");
        script.append("    }\n");
        script.append("    AssertionResult.setFailure(true);\n");
        script.append("}\n");
        script.append("AssertionResult.setFailureMessage(msg + \"&&&\" + variableValue);\n");
        return script.toString();
    }
}
