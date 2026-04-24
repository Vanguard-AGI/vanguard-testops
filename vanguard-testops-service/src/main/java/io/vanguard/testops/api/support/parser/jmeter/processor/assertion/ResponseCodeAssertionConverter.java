package io.vanguard.testops.api.support.parser.jmeter.processor.assertion;

import io.vanguard.testops.plugin.api.dto.ParameterConfig;
import io.vanguard.testops.project.api.assertion.MsResponseCodeAssertion;
import io.vanguard.testops.sdk.dto.api.result.ResponseAssertionResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.assertions.ResponseAssertion;
import org.apache.jorphan.collections.HashTree;

/**
 * @Author: Jan
 * @CreateTime: 2023-12-27  21:01
 */
public class ResponseCodeAssertionConverter extends AssertionConverter<MsResponseCodeAssertion> {
    @Override
    public void parse(HashTree hashTree, MsResponseCodeAssertion msAssertion, ParameterConfig config, boolean isIgnoreStatus) {
        if (!needParse(msAssertion, config) || !isValid(msAssertion)) {
            return;
        }
        hashTree.add(parse2ResponseAssertion(msAssertion, isIgnoreStatus));
    }

    public boolean isValid(MsResponseCodeAssertion msAssertion) {
        return StringUtils.isNotBlank(msAssertion.getCondition());
    }

    private ResponseAssertion parse2ResponseAssertion(MsResponseCodeAssertion msAssertion, boolean isIgnoreStatus) {
        ResponseAssertion assertion = createResponseAssertion();
        String expectedValue = msAssertion.getExpectedValue();
        assertion.setEnabled(msAssertion.getEnable());
        assertion.setAssumeSuccess(isIgnoreStatus);
        assertion.setEnabled(msAssertion.getEnable());

        String condition = msAssertion.getCondition();
        assertion.setName(String.format("Response code %s %s", condition.toLowerCase().replace("_", ""), expectedValue));
        assertion.addTestString(generateRegexExpression(condition, expectedValue));
        assertion.setToContainsType();
        assertion.setTestFieldResponseCode();

        setMsAssertionInfoProperty(assertion, ResponseAssertionResult.AssertionResultType.RESPONSE_CODE.name(), expectedValue, condition, expectedValue);
        return assertion;
    }
}
