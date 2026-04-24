package io.vanguard.testops.api.support.parser.ms.http.controller;

import io.vanguard.testops.api.dto.request.controller.MsIfController;
import io.vanguard.testops.plugin.api.spi.AbstractMsElementConverter;
import io.vanguard.testops.plugin.api.spi.AbstractMsTestElement;
import io.vanguard.testops.sdk.constants.MsAssertionCondition;
import org.apache.jmeter.control.IfController;
import org.apache.jorphan.collections.HashTree;

public class IfControllerConverter extends AbstractMsElementConverter<IfController> {
    @Override
    public void toMsElement(AbstractMsTestElement parent, IfController element, HashTree hashTree) {
        MsIfController msIfController = new MsIfController();
        msIfController.setCondition(MsAssertionCondition.EMPTY.name());
        msIfController.setVariable(element.getCondition());
        parent.getChildren().add(msIfController);

        parseChild(msIfController, element, hashTree);
    }
}
