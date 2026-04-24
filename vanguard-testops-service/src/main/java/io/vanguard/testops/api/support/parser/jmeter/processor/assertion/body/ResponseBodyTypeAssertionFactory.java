package io.vanguard.testops.api.support.parser.jmeter.processor.assertion.body;

import io.vanguard.testops.project.api.assertion.body.MsDocumentAssertion;
import io.vanguard.testops.project.api.assertion.body.MsJSONPathAssertion;
import io.vanguard.testops.project.api.assertion.body.MsRegexAssertion;
import io.vanguard.testops.project.api.assertion.body.MsXPathAssertion;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: Jan
 * @CreateTime: 2024-01-03  09:42
 */
public class ResponseBodyTypeAssertionFactory {

    private static final Map<Class<?>, ResponseBodyTypeAssertionConverter> converterMap = new HashMap<>();

    static {
        converterMap.put(MsJSONPathAssertion.class, new JSONPathAssertionConverter());
        converterMap.put(MsXPathAssertion.class, new XPathAssertionConverter());
        converterMap.put(MsDocumentAssertion.class, new DocumentAssertionConverter());
        converterMap.put(MsRegexAssertion.class, new RegexAssertionConverter());
    }

    public static ResponseBodyTypeAssertionConverter getConverter(Class<?> processorClass) {
        return converterMap.get(processorClass);
    }
}
