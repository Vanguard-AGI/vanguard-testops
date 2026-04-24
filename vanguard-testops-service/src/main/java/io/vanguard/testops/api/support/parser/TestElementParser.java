package io.vanguard.testops.api.support.parser;


import io.vanguard.testops.plugin.api.dto.ParameterConfig;
import io.vanguard.testops.plugin.api.spi.AbstractMsTestElement;

/**
 * @Author: Jan
 * @CreateTime: 2023-10-27  10:07
 *
 * 执行对象解析器
 */
public interface TestElementParser {

     /**
      * 将 MsTestElement 转换为对应执行引擎的执行对象
      * @param msTestElement
      * @param config
      * @return
      */
     String parse(AbstractMsTestElement msTestElement, ParameterConfig config);
}
