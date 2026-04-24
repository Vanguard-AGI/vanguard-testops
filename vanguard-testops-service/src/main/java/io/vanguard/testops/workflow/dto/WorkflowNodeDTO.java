package io.vanguard.testops.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 工作流节点 DTO
 * 对应前端的 WorkflowNode 接口
 */
@Data
public class WorkflowNodeDTO {

    @Schema(description = "节点ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "节点类型: http/sql/dubbo/script/websocket/condition", requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;

    @Schema(description = "节点名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "节点描述")
    private String description;

    @Schema(description = "节点配置（API请求参数、SQL语句等）")
    private Map<String, Object> config;

    // ⭐ 画布坐标 - 前端关键字段
    @Schema(description = "节点在画布上的X坐标", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double x;

    @Schema(description = "节点在画布上的Y坐标", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double y;

    // 可选字段
    @Schema(description = "排序顺序")
    private Long orderNum;

    @Schema(description = "引用模式: NONE/COPY/REF_METADATA/REF_WORKFLOW")
    private String refMode;

    @Schema(description = "关联的元数据ID")
    private String refMetadataId;

    @Schema(description = "关联的工作流ID")
    private String refWorkflowId;

    @Schema(description = "是否启用")
    private Boolean enable;
}

