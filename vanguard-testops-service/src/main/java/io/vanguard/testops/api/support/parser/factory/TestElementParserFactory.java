package io.vanguard.testops.api.support.parser.factory;

import io.vanguard.testops.api.support.parser.jmeter.JmeterTestElementParser;
import io.vanguard.testops.api.support.parser.TestElementParser;

/**
 * @Author: Jan
 * @CreateTime: 2023-10-30  10:59
 * 解析器工厂
 *
 */
public class TestElementParserFactory {

    /**
     * 获取默认解析器
     * @return
     */
    public static TestElementParser getDefaultParser() {
        return new JmeterTestElementParser();
    }
}
