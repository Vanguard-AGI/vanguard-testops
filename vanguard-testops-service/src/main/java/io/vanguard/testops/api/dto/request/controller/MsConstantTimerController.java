package io.vanguard.testops.api.dto.request.controller;

import io.vanguard.testops.plugin.api.spi.AbstractMsTestElement;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MsConstantTimerController extends AbstractMsTestElement {
    /**
     * 延迟时间
     */
    private Long delay;
}
