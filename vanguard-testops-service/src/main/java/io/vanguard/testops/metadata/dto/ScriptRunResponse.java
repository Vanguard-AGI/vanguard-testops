package io.vanguard.testops.metadata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 脚本执行统一返回格式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScriptRunResponse {

    @Schema(description = "状态码", example = "200")
    private int code;

    @Schema(description = "提示信息", example = "success")
    private String message;

    @Schema(description = "数据：仅返回 results 列表")
    private Object data;

    public static ScriptRunResponse success(Object data) {
        return new ScriptRunResponse(200, "success", data);
    }

    public static ScriptRunResponse fail(int code, String message, Object data) {
        return new ScriptRunResponse(code, message, data);
    }
}
