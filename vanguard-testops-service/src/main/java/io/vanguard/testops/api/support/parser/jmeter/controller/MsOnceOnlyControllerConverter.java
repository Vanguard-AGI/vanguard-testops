package io.vanguard.testops.api.support.parser.jmeter.controller;

import io.vanguard.testops.api.dto.request.controller.MsOnceOnlyController;
import io.vanguard.testops.plugin.api.dto.ParameterConfig;
import io.vanguard.testops.plugin.api.spi.AbstractJmeterElementConverter;
import io.vanguard.testops.sdk.util.LogUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.control.OnceOnlyController;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jorphan.collections.HashTree;

public class MsOnceOnlyControllerConverter extends AbstractJmeterElementConverter<MsOnceOnlyController> {


    @Override
    public void toHashTree(HashTree tree, MsOnceOnlyController element, ParameterConfig config) {
        if (BooleanUtils.isFalse(element.getEnable())) {
            LogUtils.info("MsOnceOnlyController is disabled");
            return;
        }

        HashTree groupTree = tree.add(onceOnlyController(element));
        parseChild(groupTree, element, config);
    }

    private OnceOnlyController onceOnlyController(MsOnceOnlyController element) {
        OnceOnlyController onceOnlyController = new OnceOnlyController();
        onceOnlyController.setEnabled(element.getEnable());
        onceOnlyController.setName(StringUtils.isNotBlank(element.getName()) ? element.getName() : "Once Only Controller");
        onceOnlyController.setProperty(TestElement.TEST_CLASS, OnceOnlyController.class.getName());
        onceOnlyController.setProperty(TestElement.GUI_CLASS, SaveService.aliasToClass("OnceOnlyControllerGui"));
        return onceOnlyController;
    }
}
