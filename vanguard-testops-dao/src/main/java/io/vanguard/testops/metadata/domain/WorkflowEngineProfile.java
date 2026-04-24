package io.vanguard.testops.metadata.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.vanguard.testops.handler.DateTimeTypeHandler;
import io.vanguard.testops.validation.groups.Created;
import io.vanguard.testops.validation.groups.Updated;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
@TableName(value = "workflow_engine_profile", autoResultMap = true)
public class WorkflowEngineProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "environment_id", type = IdType.ASSIGN_ID)
    @Schema(description = "配置ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{workflow_engine_profile.environment_id.not_blank}", groups = {Updated.class})
    @Size(min = 1, max = 50, message = "{workflow_engine_profile.environment_id.length_range}", groups = {Created.class, Updated.class})
    private String id;

    @TableField("project_id")
    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{workflow_engine_profile.project_id.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 50, message = "{workflow_engine_profile.project_id.length_range}", groups = {Created.class, Updated.class})
    private String projectId;

    @TableField("environment_name")
    @Schema(description = "配置名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{workflow_engine_profile.environment_name.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 255, message = "{workflow_engine_profile.environment_name.length_range}", groups = {Created.class, Updated.class})
    private String name;

    @TableField("engine_type")
    @Schema(description = "引擎类型: API/UI", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{workflow_engine_profile.engine_type.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 50, message = "{workflow_engine_profile.engine_type.length_range}", groups = {Created.class, Updated.class})
    private String engineType;

    @TableField("env_code")
    @Schema(description = "环境: DEV/TEST/PROD", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{workflow_engine_profile.env_code.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 32, message = "{workflow_engine_profile.env_code.length_range}", groups = {Created.class, Updated.class})
    private String envCode;

    @TableField(value = "robots", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "机器人")
    private Map<String, Object> robots;

    @TableField(value = "data_endpoint", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "被测服务数据库及各种数据连接")
    private Map<String, Object> dataEndpoint;

    @TableField(value = "variables", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "公共参数", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{workflow_engine_profile.variables.not_blank}", groups = {Created.class})
    private Map<String, Object> variables;

    @TableField("create_user")
    @Schema(description = "创建人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{workflow_engine_profile.create_user.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 50, message = "{workflow_engine_profile.create_user.length_range}", groups = {Created.class, Updated.class})
    private String createUser;

    @TableField("update_user")
    @Schema(description = "最后修改人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{workflow_engine_profile.update_user.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 50, message = "{workflow_engine_profile.update_user.length_range}", groups = {Created.class, Updated.class})
    private String updateUser;

    @TableField("create_time")
    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{workflow_engine_profile.create_time.not_blank}", groups = {Created.class})
    private Long createTime;

    @TableField("update_time")
    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{workflow_engine_profile.update_time.not_blank}", groups = {Created.class})
    private Long updateTime;

    @TableField(value = "deleted_time", typeHandler = DateTimeTypeHandler.class)
    @Schema(description = "删除时间")
    private Long deletedTime;

    @TableField("domain")
    @Schema(description = "服务域名/IP地址")
    @Size(max = 200, message = "{workflow_engine_profile.domain.length_range}", groups = {Created.class, Updated.class})
    private String domain;

    @TableField(value = "xxljob_info", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "XXL-Job配置信息（JSON格式，含url、账号、规则等）")
    private Map<String, Object> xxljobInfo;

    @TableField(value = "mq_info", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "MQ配置信息（JSON格式，含url信息等）")
    private Map<String, Object> mqInfo;

    @TableField(value = "dubbo_info", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "Dubbo调用信息")
    private Map<String, Object> dubboInfo;
}

