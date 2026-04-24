package io.vanguard.testops.project.dto.environment;

import io.vanguard.testops.project.api.KeyValueEnableParam;
import io.vanguard.testops.project.dto.environment.variables.CommonVariables;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class GlobalParams implements Serializable {

    @Schema(description = "请求头")
    private List<KeyValueEnableParam> headers;
    @Schema(description = "全局变量")
    private List<CommonVariables> commonVariables;

    @Serial
    private static final long serialVersionUID = 1L;
}
