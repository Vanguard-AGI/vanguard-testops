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
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "metadata_definition", autoResultMap = true)
public class MetadataDefinition implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "definition_id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{metadata_definition.definition_id.not_blank}", groups = {Updated.class})
    @Size(min = 1, max = 50, message = "{metadata_definition.definition_id.length_range}", groups = {Created.class, Updated.class})
    private String id;

    @TableField("project_id")
    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{metadata_definition.project_id.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 50, message = "{metadata_definition.project_id.length_range}", groups = {Created.class, Updated.class})
    private String projectId;

    @TableField("module_id")
    @Schema(description = "模块ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{metadata_definition.module_id.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 50, message = "{metadata_definition.module_id.length_range}", groups = {Created.class, Updated.class})
    private String moduleId;

    @Schema(description = "名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{metadata_definition.name.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 255, message = "{metadata_definition.name.length_range}", groups = {Created.class, Updated.class})
    private String name;

    @Schema(description = "协议类型: HTTP/SQL/DUBBO/SCRIPT/FILE/MQ", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{metadata_definition.protocol.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 20, message = "{metadata_definition.protocol.length_range}", groups = {Created.class, Updated.class})
    private String protocol;

    @Schema(description = "版本号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{metadata_definition.version.not_blank}", groups = {Created.class})
    private Integer version;

    @TableField("is_latest")
    @Schema(description = "是否最新版本", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{metadata_definition.is_latest.not_blank}", groups = {Created.class})
    private Boolean isLatest;

    @TableField("is_case")
    @Schema(description = "是否为案例：0-否，1-是")
    private Boolean isCase;

    @Schema(description = "描述")
    @Size(max = 1000, message = "{metadata_definition.description.length_range}", groups = {Created.class, Updated.class})
    private String description;

    @TableField(value = "request_config", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "请求配置(URL/Method/Headers/Body)")
    private Map<String, Object> requestConfig;

    @TableField(value = "response_config", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "响应配置(Schema/Extract)")
    private Map<String, Object> responseConfig;

    @TableField("script_content")
    @Schema(description = "SQL语句 / Python脚本 / Shell脚本")
    private String scriptContent;

    @TableField(value = "tags", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "标签")
    private List<String> tags;

    @TableField("create_user")
    @Schema(description = "创建人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{metadata_definition.create_user.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 50, message = "{metadata_definition.create_user.length_range}", groups = {Created.class, Updated.class})
    private String createUser;

    @TableField("create_time")
    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{metadata_definition.create_time.not_blank}", groups = {Created.class})
    private Long createTime;

    @TableField("update_time")
    @Schema(description = "最后修改时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{metadata_definition.update_time.not_blank}", groups = {Created.class})
    private Long updateTime;

    @TableField(value = "deleted_time", typeHandler = DateTimeTypeHandler.class)
    @Schema(description = "删除时间")
    private Long deletedTime;
}
