package io.vanguard.testops.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 工作流公共节点 DTO
 * 用于返回给前端展示
 */
@Data
public class WorkflowPublicNodeDTO {

    @Schema(description = "节点ID")
    private String id;

    @Schema(description = "项目ID")
    private String projectId;

    @Schema(description = "节点名称")
    private String name;

    @Schema(description = "节点描述")
    private String description;

    @Schema(description = "节点类型: http_request/mysql/dubbo/script/condition/loop等")
    private String type;

    @Schema(description = "节点分类: api/data/logic/script/other")
    private String category;

    @Schema(description = "节点配置（JSON格式）")
    private Map<String, Object> config;

    @Schema(description = "创建时间（毫秒）")
    private Long createTime;

    @Schema(description = "更新时间（毫秒）")
    private Long updateTime;

    @Schema(description = "创建人")
    private String createUser;

    @Schema(description = "更新人")
    private String updateUser;
}

