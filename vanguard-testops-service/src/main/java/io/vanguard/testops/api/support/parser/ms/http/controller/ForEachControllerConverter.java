package io.vanguard.testops.api.support.parser.ms.http.controller;

import io.vanguard.testops.api.dto.request.controller.LoopType;
import io.vanguard.testops.api.dto.request.controller.MsLoopController;
import io.vanguard.testops.api.dto.request.controller.loop.MsCountController;
import io.vanguard.testops.api.dto.request.controller.loop.MsForEachController;
import io.vanguard.testops.api.dto.request.controller.loop.MsWhileController;
import io.vanguard.testops.plugin.api.spi.AbstractMsElementConverter;
import io.vanguard.testops.plugin.api.spi.AbstractMsTestElement;
import org.apache.jmeter.control.ForeachController;
import org.apache.jorphan.collections.HashTree;

public class ForEachControllerConverter extends AbstractMsElementConverter<ForeachController> {
    @Override
    public void toMsElement(AbstractMsTestElement parent, ForeachController element, HashTree hashTree) {
        MsLoopController msLoopController = new MsLoopController();
        msLoopController.setLoopType(LoopType.FOREACH.name());
        MsForEachController controller = new MsForEachController();
        controller.setValue(element.getInputValString());

        msLoopController.setForEachController(controller);
        msLoopController.setMsCountController(new MsCountController());
        msLoopController.setWhileController(new MsWhileController());

        parent.getChildren().add(msLoopController);

        parseChild(msLoopController, element, hashTree);
    }
}
