package io.vanguard.testops.api.support.parser.ms;


import io.vanguard.testops.plugin.api.spi.AbstractMsElementConverter;
import io.vanguard.testops.plugin.api.spi.AbstractMsTestElement;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jorphan.collections.HashTree;

/**
 * @Author: Jan
 * @CreateTime: 2023-10-27  10:07
 * <p>
 * 脚本解析器
 */
public class ResultCollectorConverter extends AbstractMsElementConverter<ResultCollector> {
    @Override
    public void toMsElement(AbstractMsTestElement parent, ResultCollector element, HashTree hashTree) {
        // resultController不做处理
    }
}
