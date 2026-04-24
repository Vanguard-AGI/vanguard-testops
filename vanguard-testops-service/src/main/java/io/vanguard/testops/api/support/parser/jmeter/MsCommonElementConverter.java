package io.vanguard.testops.api.support.parser.jmeter;


import io.vanguard.testops.api.constants.ApiConstants;
import io.vanguard.testops.api.dto.ApiParamConfig;
import io.vanguard.testops.api.dto.assertion.MsAssertionConfig;
import io.vanguard.testops.api.dto.request.MsCommonElement;
import io.vanguard.testops.api.dto.request.http.MsHTTPElement;
import io.vanguard.testops.api.dto.request.processors.MsProcessorConfig;
import io.vanguard.testops.api.support.parser.jmeter.processor.MsProcessorConverter;
import io.vanguard.testops.api.support.parser.jmeter.processor.MsProcessorConverterFactory;
import io.vanguard.testops.api.support.parser.jmeter.processor.assertion.AssertionConverterFactory;
import io.vanguard.testops.plugin.api.dto.ParameterConfig;
import io.vanguard.testops.plugin.api.spi.AbstractJmeterElementConverter;
import io.vanguard.testops.plugin.api.spi.AbstractMsTestElement;
import io.vanguard.testops.project.api.assertion.MsAssertion;
import io.vanguard.testops.project.api.assertion.MsResponseCodeAssertion;
import io.vanguard.testops.project.api.processor.MsProcessor;
import io.vanguard.testops.project.dto.environment.EnvironmentConfig;
import io.vanguard.testops.project.dto.environment.EnvironmentInfoDTO;
import io.vanguard.testops.project.dto.environment.processors.ApiEnvProcessorConfig;
import io.vanguard.testops.project.dto.environment.processors.ApiEnvRequestProcessorConfig;
import io.vanguard.testops.project.dto.environment.processors.EnvProcessorConfig;
import io.vanguard.testops.project.dto.environment.processors.EnvRequestScriptProcessor;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.jorphan.collections.HashTree;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @Author: Jan
 * @CreateTime: 2023-10-27  10:07
 * <p>
 * 脚本解析器
 */
public class MsCommonElementConverter extends AbstractJmeterElementConverter<MsCommonElement> {


    @Override
    public void toHashTree(HashTree tree, MsCommonElement element, ParameterConfig config) {
        element.setProjectId(element.getParent().getProjectId());
        EnvironmentInfoDTO envInfo = getEnvInfo(element, config);
        // 解析前置处理器，包括环境前置
        addProcessors(tree, element, config, envInfo, true);
        // 解析后置处理器，包括环境后置
        addProcessors(tree, element, config, envInfo, false);
        // 处理断言，包括环境断言
        addAssertion(tree, element, config);
    }

    private EnvironmentInfoDTO getEnvInfo(MsCommonElement element, ParameterConfig config) {
        if (config instanceof ApiParamConfig) {
            ApiParamConfig apiParamConfig = (ApiParamConfig) config;
            return apiParamConfig.getEnvConfig(element.getProjectId());
        }
        return null;
    }

    /**
     * 添加断言
     *
     * @param tree
     * @param element
     * @param config
     */
    private void addAssertion(HashTree tree, MsCommonElement element, ParameterConfig config) {
        MsAssertionConfig assertionConfig = element.getAssertionConfig();
        List<MsAssertion> assertions = assertionConfig.getAssertions();

        // 添加环境断言
        if (assertionConfig.getEnableGlobal() && config instanceof ApiParamConfig) {
            ApiParamConfig apiParamConfig = (ApiParamConfig) config;
            EnvironmentInfoDTO envConfig = apiParamConfig.getEnvConfig(element.getProjectId());
            if (envConfig != null) {
                assertions.addAll(envConfig.getConfig().getAssertionConfig().getAssertions());
            }
        }

        // 将状态码断言放最前面，否则会影响脚本断言的效果，即使脚本断言失败，总状态还是显示成功
        List<MsAssertion> sortAssertions = new ArrayList<>(assertions.size());
        assertions.forEach(item -> {
            if (BooleanUtils.isTrue(item.getEnable()) && item instanceof MsResponseCodeAssertion) {
                sortAssertions.add(item);
            }
        });
        assertions.forEach(item -> {
            if (BooleanUtils.isTrue(item.getEnable()) && !(item instanceof MsResponseCodeAssertion)) {
                sortAssertions.add(item);
            }
        });
        for (int i = 0; i < sortAssertions.size(); i++) {
            MsAssertion assertion = sortAssertions.get(i);
            assertion.setProjectId(element.getProjectId());
            // 只给第一个响应码断言设置忽略状态
            boolean isIgnoreStatus = i == 0 && assertion instanceof MsResponseCodeAssertion;

            AssertionConverterFactory.getConverter(assertion.getClass())
                    .parse(tree, assertion, config, isIgnoreStatus);
        }
    }

    private void addProcessors(HashTree tree, MsCommonElement msCommonElement, ParameterConfig config,
                               EnvironmentInfoDTO envInfo, boolean isPre) {
        MsProcessorConfig processorConfig = isPre ? msCommonElement.getPreProcessorConfig() : msCommonElement.getPostProcessorConfig();
        if (processorConfig == null) {
            processorConfig = new MsProcessorConfig();
        }
        AbstractMsTestElement parent = msCommonElement.getParent();
        String protocol = null;
        if (parent instanceof MsHTTPElement) {
            protocol = ApiConstants.HTTP_PROTOCOL;
        } else {
            if (config instanceof ApiParamConfig) {
                ApiParamConfig apiParamConfig = (ApiParamConfig) config;
                protocol = apiParamConfig.getTestElementClassProtocolMap().get(parent.getClass());
            }
        }

        List<MsProcessor> beforeStepProcessors = new ArrayList<>(0);
        List<MsProcessor> afterStepProcessors = new ArrayList<>(0);

        // 开启全局前置才处理环境前置处理器
        if (BooleanUtils.isTrue(processorConfig.getEnableGlobal()) && envInfo != null) {
            EnvironmentConfig envConfig = envInfo.getConfig();
            EnvProcessorConfig envProcessorConfig = isPre ? envConfig.getPreProcessorConfig() : envConfig.getPostProcessorConfig();
            addEnvProcessors(envProcessorConfig, beforeStepProcessors, afterStepProcessors, protocol, isPre);
        }

        Function<Class<?>, MsProcessorConverter<MsProcessor>> getConverterFunc =
                isPre ? MsProcessorConverterFactory::getPreConverter : MsProcessorConverterFactory::getPostConverter;

        // 处理环境中，步骤前处理器
        beforeStepProcessors.stream()
                .filter(MsProcessor::getEnable)
                .forEach(processor -> {
            processor.setProjectId(msCommonElement.getProjectId());
            getConverterFunc.apply(processor.getClass()).parse(tree, processor, config);
        });

        processorConfig.getProcessors().stream()
                .filter(MsProcessor::getEnable)
                .forEach(processor -> {
                    processor.setProjectId(msCommonElement.getProjectId());
                    getConverterFunc.apply(processor.getClass()).parse(tree, processor, config);
                });

        // 处理环境中，步骤后处理器
        afterStepProcessors.stream()
                .filter(MsProcessor::getEnable)
                .forEach(processor -> {
            processor.setProjectId(msCommonElement.getProjectId());
            getConverterFunc.apply(processor.getClass()).parse(tree, processor, config);
        });
    }

    private void addEnvProcessors(EnvProcessorConfig envProcessorConfig,
                                  List<MsProcessor> beforeStepProcessors,
                                  List<MsProcessor> afterStepProcessors,
                                  String protocol,
                                  boolean isPre) {
        // 获取环境中的前后置处理器
        ApiEnvProcessorConfig apiProcessorConfig = envProcessorConfig.getApiProcessorConfig();
        ApiEnvRequestProcessorConfig requestProcessorConfig = apiProcessorConfig.getRequestProcessorConfig();
        List<MsProcessor> processors = requestProcessorConfig.getProcessors();

        for (MsProcessor processor : processors) {
            if (processor instanceof EnvRequestScriptProcessor) {
                EnvRequestScriptProcessor requestScriptProcessor = (EnvRequestScriptProcessor) processor;
                // 如果是脚本处理器，处理步骤内前置脚本前后配置，以及忽略的协议
                Boolean beforeStepScript = requestScriptProcessor.getBeforeStepScript();
                List<String> ignoreProtocols = requestScriptProcessor.getIgnoreProtocols();
                if (ignoreProtocols.contains(protocol)) {
                    return;
                }
                if (BooleanUtils.isTrue(beforeStepScript)) {
                    beforeStepProcessors.add(processor);
                } else {
                    afterStepProcessors.add(processor);
                }
            } else {
                // 其他处理器
                if (isPre) {
                    // 如果是前置，则在前置处理器之前执行
                    beforeStepProcessors.add(processor);
                } else {
                    // 如果是后置，则在后置处理器之后执行
                    afterStepProcessors.add(processor);
                }
            }
        }
    }
}
