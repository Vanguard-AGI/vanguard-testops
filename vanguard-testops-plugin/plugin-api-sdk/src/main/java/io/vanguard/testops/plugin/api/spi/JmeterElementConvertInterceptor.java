package io.vanguard.testops.plugin.api.spi;

import io.vanguard.testops.plugin.api.dto.ParameterConfig;
import org.apache.jorphan.collections.HashTree;

/**
 * @Author: Jan
 * @CreateTime: 2024-06-16  19:23
 */
public interface JmeterElementConvertInterceptor {

    HashTree intercept(HashTree tree, MsTestElement element, ParameterConfig config);
}
