package io.vanguard.testops.workflow.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 工作流公共节点实体类
 * 用于存储项目中可复用的节点模板
 */
@Data
@TableName(value = "workflow_public_node", autoResultMap = true)
public class WorkflowPublicNode implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "节点ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "节点ID不能为空")
    @Size(min = 1, max = 50, message = "节点ID长度范围1-50")
    private String id;

    @TableField("project_id")
    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "项目ID不能为空")
    @Size(min = 1, max = 50, message = "项目ID长度范围1-50")
    private String projectId;

    @TableField("name")
    @Schema(description = "节点名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "节点名称不能为空")
    @Size(min = 1, max = 255, message = "节点名称长度范围1-255")
    private String name;

    @TableField("description")
    @Schema(description = "节点描述")
    @Size(max = 1000, message = "节点描述长度不能超过1000")
    private String description;

    @TableField("type")
    @Schema(description = "节点类型: http_request/mysql/dubbo/script/condition/loop等", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "节点类型不能为空")
    @Size(max = 50, message = "节点类型长度不能超过50")
    private String type;

    @TableField("category")
    @Schema(description = "节点分类: api/data/logic/script/other", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "节点分类不能为空")
    @Size(max = 20, message = "节点分类长度不能超过20")
    private String category;

    @TableField(value = "config", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "节点配置（JSON格式，包含节点的完整配置信息）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Object> config;

    @TableField("create_time")
    @Schema(description = "创建时间（毫秒）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long createTime;

    @TableField("update_time")
    @Schema(description = "更新时间（毫秒）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long updateTime;

    @TableField("create_user")
    @Schema(description = "创建人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "创建人不能为空")
    @Size(max = 50, message = "创建人长度不能超过50")
    private String createUser;

    @TableField("update_user")
    @Schema(description = "更新人")
    @Size(max = 50, message = "更新人长度不能超过50")
    private String updateUser;

    @TableField("deleted_time")
    @Schema(description = "删除时间（软删除，NULL表示未删除）")
    private LocalDateTime deletedTime;

    @TableField("deleted_by")
    @Schema(description = "删除人")
    @Size(max = 50, message = "删除人长度不能超过50")
    private String deletedBy;
}

