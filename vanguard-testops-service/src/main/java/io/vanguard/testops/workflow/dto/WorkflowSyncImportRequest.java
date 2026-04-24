package io.vanguard.testops.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 工作流同步导入请求 DTO
 * 用于将HTTP和SQL请求转换为工作流节点并导入
 */
@Data
public class WorkflowSyncImportRequest {

    @Schema(description = "工作空间ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "工作空间ID不能为空")
    @Size(min = 1, max = 50, message = "工作空间ID长度范围1-50")
    private String workspaceId;

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "项目ID不能为空")
    @Size(min = 1, max = 50, message = "项目ID长度范围1-50")
    private String projectId;

    @Schema(description = "工作流名称")
    @Size(max = 255, message = "工作流名称长度不能超过255")
    private String workflowName;

    @Schema(description = "工作流描述")
    @Size(max = 1000, message = "工作流描述长度不能超过1000")
    private String description;

    @Schema(description = "节点列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "节点列表不能为空")
    private List<SyncNodeData> nodes;

    /**
     * 同步节点数据
     */
    @Data
    public static class SyncNodeData {
        @Schema(description = "节点类型: http_request/sql", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "节点类型不能为空")
        private String type;

        @Schema(description = "节点名称")
        @Size(max = 255, message = "节点名称长度不能超过255")
        private String name;

        // HTTP节点字段
        @Schema(description = "HTTP方法 (GET/POST/PUT/DELETE等)")
        private String method;

        @Schema(description = "HTTP完整URL")
        private String url;

        @Schema(description = "HTTP请求路径")
        private String path;

        @Schema(description = "HTTP请求头")
        private Map<String, String> headers;

        @Schema(description = "HTTP查询参数")
        private Map<String, String> queryParams;

        @Schema(description = "HTTP请求体（字符串）")
        private String body;

        @Schema(description = "HTTP JSON请求体")
        private Object json;

        @Schema(description = "HTTP表单数据")
        private Map<String, Object> data;

        // SQL节点字段
        @Schema(description = "SQL语句")
        private String sql;

        @Schema(description = "SQL操作类型 (SELECT/INSERT/UPDATE/DELETE等)")
        private String operationType;
    }
}

