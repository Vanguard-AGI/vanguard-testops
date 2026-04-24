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
 * 工作流定义实体类
 */
@Data
@TableName(value = "workflow_definition", autoResultMap = true)
public class WorkflowDefinition implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "workflow_id", type = IdType.ASSIGN_ID)
    @Schema(description = "工作流ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "工作流ID不能为空")
    @Size(min = 1, max = 50, message = "工作流ID长度范围1-50")
    private String workflowId;

    @TableField("project_id")
    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "项目ID不能为空")
    @Size(min = 1, max = 50, message = "项目ID长度范围1-50")
    private String projectId;

    @TableField("module_id")
    @Schema(description = "模块ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "模块ID不能为空")
    @Size(min = 1, max = 50, message = "模块ID长度范围1-50")
    private String moduleId;

    @Schema(description = "工作流名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "工作流名称不能为空")
    @Size(min = 1, max = 255, message = "工作流名称长度范围1-255")
    private String name;

    @Schema(description = "工作流分类: API/UI/AGENT")
    @Size(max = 32, message = "分类长度不能超过32")
    private String category;

    @Schema(description = "类型: TEST_CASE(测试用例) / PUBLIC_STEP(公共步骤)")
    @Size(max = 20, message = "类型长度不能超过20")
    private String type;

    @Schema(description = "版本号")
    private Integer version;

    @TableField(value = "global_vars", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "全局变量定义")
    private Map<String, Object> globalVars;

    @TableField("environment_id")
    @Schema(description = "环境ID")
    @Size(max = 50, message = "环境ID长度不能超过50")
    private String environmentId;

    @TableField(value = "schedule_config", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "定时触发配置 (Cron等)")
    private Map<String, Object> scheduleConfig;

    @Schema(description = "描述")
    @Size(max = 1000, message = "描述长度不能超过1000")
    private String description;

    @Schema(description = "状态: DRAFT/PUBLISHED")
    @Size(max = 20, message = "状态长度不能超过20")
    private String status;

    @TableField("create_user")
    @Schema(description = "创建人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "创建人不能为空")
    @Size(min = 1, max = 50, message = "创建人长度范围1-50")
    private String createUser;

    @TableField("create_time")
    @Schema(description = "创建时间（毫秒）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long createTime;

    @TableField("update_time")
    @Schema(description = "最后修改时间（毫秒）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long updateTime;

    @TableField("update_user")
    @Schema(description = "最后修改人")
    @Size(max = 50, message = "修改人长度不能超过50")
    private String updateUser;

    @TableField("deleted_time")
    @Schema(description = "删除时间（软删除，NULL表示未删除）")
    private LocalDateTime deletedTime;
}

