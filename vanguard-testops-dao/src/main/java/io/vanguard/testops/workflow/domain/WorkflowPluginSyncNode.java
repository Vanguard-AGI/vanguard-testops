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
 * 外部插件同步节点数据实体类
 */
@Data
@TableName(value = "workflow_plugin_sync_node", autoResultMap = true)
public class WorkflowPluginSyncNode implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "node_id", type = IdType.ASSIGN_ID)
    @Schema(description = "节点ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "节点ID不能为空")
    @Size(min = 1, max = 50, message = "节点ID长度范围1-50")
    private String nodeId;

    @TableField("email")
    @Schema(description = "用户邮箱", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户邮箱不能为空")
    @Size(max = 255, message = "用户邮箱长度不能超过255")
    private String email;

    @TableField("node_type")
    @Schema(description = "节点类型: HTTP/SQL/DUBBO/ROCKETMQ", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "节点类型不能为空")
    @Size(max = 20, message = "节点类型长度不能超过20")
    private String nodeType;

    @TableField(value = "endpoint_data", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "节点数据（JSON格式，包含endpoint的完整信息）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Object> endpointData;

    @TableField("create_time")
    @Schema(description = "创建时间（毫秒）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long createTime;

    @TableField("update_time")
    @Schema(description = "更新时间（毫秒）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long updateTime;

    @TableField("deleted_time")
    @Schema(description = "删除时间（软删除，NULL表示未删除）")
    private LocalDateTime deletedTime;
}

