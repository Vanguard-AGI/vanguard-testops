package io.vanguard.testops.metadata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Swagger 导入请求")
public class SwaggerImportRequest {

    @Schema(description = "Swagger URL", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Swagger URL不能为空")
    private String url;

    @Schema(description = "服务代码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "服务代码不能为空")
    @Size(min = 1, max = 100, message = "服务代码长度必须在1-100之间")
    private String serviceCode;

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "项目ID不能为空")
    @Size(min = 1, max = 50, message = "项目ID长度必须在1-50之间")
    private String projectId;

    @Schema(description = "模块ID")
    @Size(max = 50, message = "模块ID长度不能超过50")
    private String moduleId;
}

