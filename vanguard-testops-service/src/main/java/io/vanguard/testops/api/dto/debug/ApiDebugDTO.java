package io.vanguard.testops.api.dto.debug;

import io.vanguard.testops.api.domain.ApiDebug;
import io.vanguard.testops.plugin.api.spi.AbstractMsTestElement;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ApiDebugDTO extends ApiDebug {
    @Schema(description = "请求内容")
    private AbstractMsTestElement request;

    @Schema(description = "响应内容")
    private String response;
}
