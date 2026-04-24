package io.vanguard.testops.api.dto.request.controller;

import io.vanguard.testops.api.dto.request.controller.loop.MsCountController;
import io.vanguard.testops.api.dto.request.controller.loop.MsForEachController;
import io.vanguard.testops.api.dto.request.controller.loop.MsWhileController;
import io.vanguard.testops.plugin.api.spi.AbstractMsTestElement;
import io.vanguard.testops.sdk.valid.EnumValue;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class MsLoopController extends AbstractMsTestElement {

    /**
     * 循环类型
     *
     * @see LoopType
     */
    @EnumValue(enumClass = LoopType.class)
    private String loopType = LoopType.LOOP_COUNT.name();
    /**
     * 次数控制器
     */
    private MsCountController msCountController;
    /**
     * ForEach控制器
     */
    private MsForEachController forEachController;
    /**
     * While控制器
     */
    private MsWhileController whileController;
    private String currentTime = UUID.randomUUID().toString();
}



