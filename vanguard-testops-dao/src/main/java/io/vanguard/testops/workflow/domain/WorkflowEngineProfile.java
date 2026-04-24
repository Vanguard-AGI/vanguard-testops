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
import java.util.List;
import java.util.Map;

/**
 * 工作流执行引擎/环境配置实体类
 */
@Data
@TableName(value = "workflow_engine_profile", autoResultMap = true)
public class WorkflowEngineProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "environment_id", type = IdType.ASSIGN_ID)
    @Schema(description = "环境ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "环境ID不能为空")
    @Size(min = 1, max = 50, message = "环境ID长度范围1-50")
    private String environmentId;

    @TableField("project_id")
    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "项目ID不能为空")
    @Size(min = 1, max = 50, message = "项目ID长度范围1-50")
    private String projectId;

    @TableField("environment_name")
    @Schema(description = "环境名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "环境名称不能为空")
    @Size(max = 255, message = "环境名称长度不能超过255")
    private String environmentName;

    @TableField("engine_type")
    @Schema(description = "引擎类型: API/UI", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "引擎类型不能为空")
    @Size(max = 50, message = "引擎类型长度不能超过50")
    private String engineType;

    @TableField(value = "auth_config", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "认证信息(Token/Key)")
    private Map<String, Object> authConfig;

    @TableField("env_code")
    @Schema(description = "环境标识: DEV/TEST/PROD", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "环境标识不能为空")
    @Size(max = 32, message = "环境标识长度不能超过32")
    private String envCode;

    @TableField(value = "robots", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "机器人配置")
    private List<Map<String, Object>> robots;

    @TableField(value = "data_endpoint", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "被测服务数据库及各种数据连接")
    private Map<String, Object> dataEndpoint;

    @TableField(value = "variables", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "公共参数", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Object> variables;

    @TableField("create_user")
    @Schema(description = "创建人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "创建人不能为空")
    @Size(max = 50, message = "创建人长度不能超过50")
    private String createUser;

    @TableField("update_user")
    @Schema(description = "最后修改人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "最后修改人不能为空")
    @Size(max = 50, message = "最后修改人长度不能超过50")
    private String updateUser;

    @TableField("create_time")
    @Schema(description = "创建时间（毫秒）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long createTime;

    @TableField("update_time")
    @Schema(description = "更新时间（毫秒）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long updateTime;

    @TableField(value = "deleted_time", typeHandler = io.vanguard.testops.handler.DateTimeTypeHandler.class)
    @Schema(description = "删除时间（软删除）")
    private Long deletedTime;
}

