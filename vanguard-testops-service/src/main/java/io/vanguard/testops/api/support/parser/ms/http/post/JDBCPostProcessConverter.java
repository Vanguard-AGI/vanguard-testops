package io.vanguard.testops.api.support.parser.ms.http.post;

import io.vanguard.testops.api.support.parser.ms.ConverterUtils;
import io.vanguard.testops.plugin.api.spi.AbstractMsElementConverter;
import io.vanguard.testops.plugin.api.spi.AbstractMsTestElement;
import org.apache.jmeter.protocol.jdbc.processor.JDBCPostProcessor;
import org.apache.jorphan.collections.HashTree;

public class JDBCPostProcessConverter extends AbstractMsElementConverter<JDBCPostProcessor> {
    @Override
    public void toMsElement(AbstractMsTestElement parent, JDBCPostProcessor element, HashTree hashTree) {
        ConverterUtils.addPostProcess(parent, ConverterUtils.genJDBCProcessor(element));
    }
}
