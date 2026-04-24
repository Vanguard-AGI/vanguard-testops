package io.vanguard.testops.plugin.api.spi;


import io.vanguard.testops.plugin.api.dto.ParameterConfig;
import org.apache.jorphan.collections.HashTree;
import org.pf4j.ExtensionPoint;

/**
 * @author Jan
 * @createTime 2026-04-22
 * 将 MsTestElement 具体实现类转换为 HashTree
 */
public interface JmeterElementConverter<T extends MsTestElement> extends ExtensionPoint {

    /**
     * 将 MsTestElement 具体实现类转换为 HashTree
     */
    void toHashTree(HashTree tree, T element, ParameterConfig config);
}
