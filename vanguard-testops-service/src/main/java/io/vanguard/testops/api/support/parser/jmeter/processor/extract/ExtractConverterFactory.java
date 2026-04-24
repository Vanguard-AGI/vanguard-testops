package io.vanguard.testops.api.support.parser.jmeter.processor.extract;

import io.vanguard.testops.project.api.processor.extract.JSONPathExtract;
import io.vanguard.testops.project.api.processor.extract.RegexExtract;
import io.vanguard.testops.project.api.processor.extract.XPathExtract;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: Jan
 * @CreateTime: 2023-12-27  10:31
 */
public class ExtractConverterFactory {
    private static final Map<Class<?>, ExtractConverter> converterMap = new HashMap<>();

    static {
        converterMap.put(RegexExtract.class, new RegexExtractConverter());
        converterMap.put(JSONPathExtract.class, new JSONPathExtractConverter());
        converterMap.put(XPathExtract.class, new XPathExtractConverter());
    }

    public static ExtractConverter getConverter(Class<?> processorClass) {
        return converterMap.get(processorClass);
    }
}
