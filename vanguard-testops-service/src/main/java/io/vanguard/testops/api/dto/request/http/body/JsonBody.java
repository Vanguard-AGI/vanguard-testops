package io.vanguard.testops.api.dto.request.http.body;

import io.vanguard.testops.api.dto.schema.JsonSchemaItem;
import jakarta.validation.Valid;
import lombok.Data;

/**
 * json 请求体
 * @Author: Jan
 * @CreateTime: 2023-11-06  18:25
 */
@Data
public class JsonBody {
    /**
     * 是否 json-schema
     * 默认false
     */
    private Boolean enableJsonSchema = false;
    /**
     * json 参数值
     */
    private String jsonValue;
    /**
     * json-schema 定义
     */
    @Valid
    private JsonSchemaItem jsonSchema;
}
