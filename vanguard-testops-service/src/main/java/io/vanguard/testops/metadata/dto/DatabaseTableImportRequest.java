package io.vanguard.testops.metadata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "数据库表DDL导入请求")
public class DatabaseTableImportRequest {

    @Schema(description = "数据库连接信息", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "数据库连接信息不能为空")
    @Valid
    private Map<String, Object> dataEndpoint;

    @Schema(description = "数据库名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "数据库名称不能为空")
    private String database;

    @Schema(description = "表名（可选，不传则导入整个数据库的所有表）")
    private String tableName;

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "项目ID不能为空")
    @Size(min = 1, max = 50, message = "项目ID长度必须在1-50之间")
    private String projectId;

    @Schema(description = "模块ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "模块ID不能为空")
    @Size(min = 1, max = 50, message = "模块ID长度必须在1-50之间")
    private String moduleId;
}

