package io.vanguard.testops.project.dto.customfunction;

import io.vanguard.testops.project.domain.CustomFunction;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author: Jan
 * @date: 2026-04-22
 * @version: 1.0
 */
@Data
public class CustomFunctionDTO extends CustomFunction {

    @Schema(description = "创建人名称")
    private String createUserName;

    @Schema(description =  "参数列表")
    private String params;

    @Schema(description =  "函数体")
    private String script;

    @Schema(description =  "执行结果")
    private String result;
}
