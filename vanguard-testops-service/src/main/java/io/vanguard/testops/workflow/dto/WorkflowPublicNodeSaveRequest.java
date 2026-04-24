package io.vanguard.testops.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 保存公共节点请求 DTO
 */
@Data
public class WorkflowPublicNodeSaveRequest {

    @Schema(description = "节点ID（更新时必填，创建时不需要）")
    @Size(max = 50, message = "节点ID长度不能超过50")
    private String id;

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "项目ID不能为空")
    @Size(min = 1, max = 50, message = "项目ID长度范围1-50")
    private String projectId;

    @Schema(description = "节点名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "节点名称不能为空")
    @Size(min = 1, max = 255, message = "节点名称长度范围1-255")
    private String name;

    @Schema(description = "节点描述")
    @Size(max = 1000, message = "节点描述长度不能超过1000")
    private String description;

    @Schema(description = "节点类型: http_request/mysql/dubbo/script/condition/loop等", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "节点类型不能为空")
    @Size(max = 50, message = "节点类型长度不能超过50")
    private String type;

    @Schema(description = "节点分类: api/data/logic/script/other", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "节点分类不能为空")
    @Size(max = 20, message = "节点分类长度不能超过20")
    private String category;

    @Schema(description = "节点配置（JSON格式）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Object> config;
}

