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
import java.util.Map;

/**
 * 工作流工作空间实体类
 */
@Data
@TableName(value = "workflow_workspace", autoResultMap = true)
public class WorkflowWorkspace implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "workspace_id", type = IdType.ASSIGN_ID)
    @Schema(description = "工作空间ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "工作空间ID不能为空")
    @Size(min = 1, max = 50, message = "工作空间ID长度范围1-50")
    private String workspaceId;

    @TableField("workspace_name")
    @Schema(description = "工作空间名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "工作空间名称不能为空")
    @Size(max = 255, message = "工作空间名称长度不能超过255")
    private String workspaceName;

    @TableField("project_id")
    @Schema(description = "归属项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "项目ID不能为空")
    @Size(min = 1, max = 50, message = "项目ID长度范围1-50")
    private String projectId;

    @TableField("owner")
    @Schema(description = "负责人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "负责人不能为空")
    @Size(max = 50, message = "负责人长度不能超过50")
    private String owner;

    @TableField(value = "global_vars", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "全局变量定义")
    private Map<String, Object> globalVars;

    @TableField("visibility")
    @Schema(description = "可见范围: PRIVATE/PROJECT/ORG", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "可见范围不能为空")
    @Size(max = 20, message = "可见范围长度不能超过20")
    private String visibility = "PRIVATE";

    @TableField("description")
    @Schema(description = "描述")
    @Size(max = 1000, message = "描述长度不能超过1000")
    private String description;

    @TableField("create_time")
    @Schema(description = "创建时间（毫秒）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long createTime;

    @TableField("create_user")
    @Schema(description = "创建人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "创建人不能为空")
    @Size(max = 50, message = "创建人长度不能超过50")
    private String createUser;

    @TableField("update_time")
    @Schema(description = "最后修改时间（毫秒）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long updateTime;

    @TableField("update_user")
    @Schema(description = "最后修改人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "最后修改人不能为空")
    @Size(max = 50, message = "最后修改人长度不能超过50")
    private String updateUser;

    @TableField("deleted_by")
    @Schema(description = "删除人")
    @Size(max = 50, message = "删除人长度不能超过50")
    private String deletedBy;

    @TableField(value = "deleted_time", typeHandler = io.vanguard.testops.handler.DateTimeTypeHandler.class)
    @Schema(description = "删除时间（软删除）")
    private Long deletedTime;

    // 扩展字段（需要数据库迁移添加）
    @TableField("icon")
    @Schema(description = "图标（emoji）")
    @Size(max = 20, message = "图标长度不能超过20")
    private String icon;

    @TableField("icon_color")
    @Schema(description = "图标背景颜色（CSS类名）")
    @Size(max = 50, message = "图标颜色长度不能超过50")
    private String iconColor;
}

