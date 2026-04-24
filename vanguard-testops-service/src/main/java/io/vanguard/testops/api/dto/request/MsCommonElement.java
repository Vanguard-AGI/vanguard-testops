package io.vanguard.testops.api.dto.request;

import io.vanguard.testops.api.dto.assertion.MsAssertionConfig;
import io.vanguard.testops.api.dto.request.processors.MsProcessorConfig;
import io.vanguard.testops.plugin.api.spi.AbstractMsTestElement;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 协议插件中通用的配置
 * <pre>
 * 添加到对应元素的 children 属性中
 * </pre>
 * @Author: Jan
 * @CreateTime: 2023-12-25  10:50
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MsCommonElement extends AbstractMsTestElement {
    /**
     * 前置处理器配置
     */
    @Valid
    private MsProcessorConfig preProcessorConfig = new MsProcessorConfig();
    /**
     * 后置处理器配置
     */
    @Valid
    private MsProcessorConfig postProcessorConfig = new MsProcessorConfig();
    /**
     * 断言配置
     */
    @Valid
    private MsAssertionConfig assertionConfig = new MsAssertionConfig();

}
