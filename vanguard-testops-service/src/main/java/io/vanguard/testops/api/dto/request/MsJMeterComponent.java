package io.vanguard.testops.api.dto.request;

import io.vanguard.testops.plugin.api.spi.AbstractMsTestElement;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MsJMeterComponent extends AbstractMsTestElement {
    private String testElementContent;
}
