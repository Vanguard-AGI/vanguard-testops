package io.vanguard.testops.metadata.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "metadata_operation_log", autoResultMap = true)
public class MetadataOperationLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "自增主键ID")
    private Long id;

    @TableField("log_id")
    @Schema(description = "日志唯一标识（UUID，全局唯一）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String logId;

    @TableField("biz_type")
    @Schema(description = "日志类型：1=aegisOne 2=aegisGo", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer bizType;

    @TableField("module_type")
    @Schema(description = "模块类型（HTTP/DUBBO/SQL/ROCKETMQ等）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String moduleType;

    @TableField("project_id")
    @Schema(description = "项目唯一标识（与业务系统项目ID关联，用于按项目隔离/审计日志）。AegisGO场景下可为空")
    private String projectId;

    @TableField("related_id")
    @Schema(description = "关联业务ID")
    private String relatedId;

    @TableField("action")
    @Schema(description = "具体操作：execute/update/create/delete", requiredMode = Schema.RequiredMode.REQUIRED)
    private String action;

    @TableField("user_email")
    @Schema(description = "操作人邮箱", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userEmail;

    @TableField("execution_time_ms")
    @Schema(description = "执行耗时（毫秒）")
    private Long executionTimeMs;

    @TableField(value = "extra_data", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "扩展字段（JSON格式）")
    private Map<String, Object> extraData;

    @TableField("created_at")
    @Schema(description = "日志创建时间")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    @Schema(description = "日志更新时间")
    private LocalDateTime updatedAt;
}

