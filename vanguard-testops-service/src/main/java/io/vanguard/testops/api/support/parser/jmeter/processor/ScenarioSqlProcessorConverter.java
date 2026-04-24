package io.vanguard.testops.api.support.parser.jmeter.processor;

import io.vanguard.testops.plugin.api.dto.ParameterConfig;
import io.vanguard.testops.project.api.processor.SQLProcessor;
import org.apache.jmeter.protocol.jdbc.sampler.JDBCSampler;
import org.apache.jorphan.collections.HashTree;

/**
 * 环境场景级前后置SQL处理器
 *
 * @Author: Jan
 * @CreateTime: 2023-12-26  14:49
 */
public class ScenarioSqlProcessorConverter extends SqlProcessorConverter {

    @Override
    public void parse(HashTree hashTree, SQLProcessor sqlProcessor, ParameterConfig config) {
        parse(hashTree, sqlProcessor, config, JDBCSampler.class);
    }
}
