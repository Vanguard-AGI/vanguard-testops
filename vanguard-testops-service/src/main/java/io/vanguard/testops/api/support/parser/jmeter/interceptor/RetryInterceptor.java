package io.vanguard.testops.api.support.parser.jmeter.interceptor;

import io.vanguard.testops.api.dto.ApiParamConfig;
import io.vanguard.testops.api.dto.request.controller.MsLoopController;
import io.vanguard.testops.api.dto.request.controller.MsScriptElement;
import io.vanguard.testops.api.support.parser.jmeter.constants.JmeterAlias;
import io.vanguard.testops.plugin.api.dto.ParameterConfig;
import io.vanguard.testops.plugin.api.spi.AbstractMsProtocolTestElement;
import io.vanguard.testops.plugin.api.spi.AbstractMsTestElement;
import io.vanguard.testops.plugin.api.spi.JmeterElementConvertInterceptor;
import io.vanguard.testops.plugin.api.spi.MsTestElement;
import io.vanguard.testops.sdk.dto.api.task.ApiRunRetryConfig;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.control.WhileController;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.visualizers.JSR223Listener;
import org.apache.jorphan.collections.HashTree;

import java.util.UUID;

/**
 * @Author: Jan
 * @CreateTime: 2024-06-16  19:34
 */
public class RetryInterceptor implements JmeterElementConvertInterceptor {

    private final String template = "String retryId = \"%s\";\n"
            + "try {\n"
            + "    String retryValueName = \"VARS_\" + retryId;\n"
            + "    String retryTimes = \"%s\";\n"
            + "    String retryInterval = \"%s\";\n"
            + "    if (prev.isSuccess()) {\n"
            + "        vars.put(retryId, \"STOPPED\");\n"
            + "    }\n"
            + "    if (vars.get(retryValueName) == null) {\n"
            + "        vars.put(retryValueName, \"0\");\n"
            + "    } else {\n"
            + "        int retryNum = Integer.parseInt(vars.get(retryValueName));\n"
            + "        sleep(Integer.parseInt(retryInterval));\n"
            + "        retryNum++;\n"
            + "        log.info(\"重试：\" + retryNum);\n"
            + "        prev.setSampleLabel(\"MsRetry_\" + retryNum + \"_\" + prev.getSampleLabel());\n"
            + "        vars.put(retryValueName, String.valueOf(retryNum));\n"
            + "    }\n"
            + "    if (vars.get(retryValueName).equals(retryTimes)) {\n"
            + "        vars.put(retryId, \"STOPPED\");\n"
            + "    }\n"
            + "} catch (Exception e) {\n"
            + "    e.printStackTrace();\n"
            + "    vars.put(retryId, \"STOPPED\");\n"
            + "}";

    @Override
    public HashTree intercept(HashTree tree, MsTestElement element, ParameterConfig config) {
        AbstractMsTestElement abstractMsTestElement = (AbstractMsTestElement) element;
        ApiParamConfig apiParamConfig = (ApiParamConfig) config;
        if (isRetryEnable(apiParamConfig) && isRetryElement(element) && !isInLoop(abstractMsTestElement)) {
           return addRetryWhileController(tree, abstractMsTestElement.getName(), apiParamConfig.getRetryConfig());
        }
        return tree;
    }

    public HashTree addRetryWhileController(HashTree tree, String name, ApiRunRetryConfig retryConfig) {
        String retryId = UUID.randomUUID().toString();
        String whileCondition = String.format("${__jexl3(\"${%s}\" != \"STOPPED\")}", retryId);
        HashTree hashTree = tree.add(getRetryWhileController(whileCondition, name));
        // 添加超时处理，防止死循环
        JSR223Listener postProcessor = new JSR223Listener();
        postProcessor.setName("Retry-controller");
        postProcessor.setProperty(TestElement.TEST_CLASS, JSR223Listener.class.getName());
        postProcessor.setProperty(TestElement.GUI_CLASS, SaveService.aliasToClass(JmeterAlias.TEST_BEAN_GUI));
        postProcessor.setProperty("scriptLanguage", "groovy");
        postProcessor.setProperty("script", getRetryScript(retryId, retryConfig));
        hashTree.add(postProcessor);
        return hashTree;
    }

    private WhileController getRetryWhileController(String condition, String name) {
        if (StringUtils.isEmpty(condition)) {
            return null;
        }
        WhileController controller = new WhileController();
        controller.setEnabled(true);
        controller.setName(StringUtils.join("RetryWhile_", name));
        controller.setProperty(TestElement.TEST_CLASS, WhileController.class.getName());
        controller.setProperty(TestElement.GUI_CLASS, SaveService.aliasToClass("WhileControllerGui"));
        controller.setCondition(condition);
        return controller;
    }

    private String getRetryScript(String retryId, ApiRunRetryConfig retryConfig) {
        return String.format(template,
                retryId,
                retryConfig.getRetryTimes(),
                retryConfig.getRetryInterval()
        );
    }

    private boolean isRetryEnable(ApiParamConfig apiParamConfig) {
        return BooleanUtils.isTrue(apiParamConfig.getRetryOnFail()) && apiParamConfig.getRetryConfig().getRetryTimes() > 0;
    }

    /**
     * 需要重试的组件
     * @param element
     * @return
     */
    private boolean isRetryElement(MsTestElement element) {
        if (element instanceof AbstractMsProtocolTestElement || element instanceof MsScriptElement) {
            return true;
        }
        return false;
    }


    public boolean isInLoop(AbstractMsTestElement msTestElement) {
        if (msTestElement != null) {
            if (msTestElement instanceof MsLoopController) {
                return true;
            }
            if (msTestElement.getParent() != null) {
                return isInLoop(msTestElement.getParent());
            }
        }
        return false;
    }
}
