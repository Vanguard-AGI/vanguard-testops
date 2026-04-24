package io.vanguard.testops.api.support.parser.ms.http.pre;

import io.vanguard.testops.api.support.parser.ms.ConverterUtils;
import io.vanguard.testops.plugin.api.spi.AbstractMsElementConverter;
import io.vanguard.testops.plugin.api.spi.AbstractMsProtocolTestElement;
import io.vanguard.testops.plugin.api.spi.AbstractMsTestElement;
import org.apache.jmeter.protocol.jdbc.processor.JDBCPreProcessor;
import org.apache.jorphan.collections.HashTree;

public class JDBCPreProcessConverter extends AbstractMsElementConverter<JDBCPreProcessor> {
    @Override
    public void toMsElement(AbstractMsTestElement parent, JDBCPreProcessor element, HashTree hashTree) {
        if (parent instanceof AbstractMsProtocolTestElement) {
            ConverterUtils.addPreProcess(parent, ConverterUtils.genJDBCProcessor(element));
        }

    }
}
