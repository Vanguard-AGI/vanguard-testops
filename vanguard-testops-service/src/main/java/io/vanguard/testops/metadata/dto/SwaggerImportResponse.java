package io.vanguard.testops.metadata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Swagger 导入响应")
public class SwaggerImportResponse {

    @Schema(description = "已存在的接口数量")
    private Integer exist;

    @Schema(description = "导入的接口数量")
    private Integer imported;

    @Schema(description = "已存在的接口路径列表")
    private List<String> existInfo;
}

