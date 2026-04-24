package io.vanguard.testops.api.support.parser.jmeter.processor.assertion;

import io.vanguard.testops.api.support.parser.jmeter.processor.assertion.body.ResponseBodyTypeAssertionConverter;
import io.vanguard.testops.api.support.parser.jmeter.processor.assertion.body.ResponseBodyTypeAssertionFactory;
import io.vanguard.testops.plugin.api.dto.ParameterConfig;
import io.vanguard.testops.project.api.assertion.MsResponseBodyAssertion;
import org.apache.jorphan.collections.HashTree;

/**
 * @Author: Jan
 * @CreateTime: 2023-12-27  21:01
 */
public class ResponseBodyAssertionConverter extends AssertionConverter<MsResponseBodyAssertion> {
    @Override
    public void parse(HashTree hashTree, MsResponseBodyAssertion msAssertion, ParameterConfig config, boolean isIgnoreStatus) {
        if (!needParse(msAssertion, config)) {
            return;
        }
        ResponseBodyTypeAssertionConverter converter = ResponseBodyTypeAssertionFactory.getConverter(msAssertion.getBodyAssertionClassByType());
        converter.parse(hashTree, msAssertion.getBodyAssertionDataByType(), config, isIgnoreStatus, msAssertion.getEnable());
    }
}
