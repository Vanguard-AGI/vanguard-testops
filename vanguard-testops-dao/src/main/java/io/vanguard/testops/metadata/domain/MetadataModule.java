package io.vanguard.testops.metadata.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.vanguard.testops.handler.DateTimeTypeHandler;
import io.vanguard.testops.validation.groups.Created;
import io.vanguard.testops.validation.groups.Updated;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("metadata_module")
public class MetadataModule implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "module_id", type = IdType.ASSIGN_ID)
    @Schema(description = "模块ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{metadata_module.module_id.not_blank}", groups = {Updated.class})
    @Size(min = 1, max = 50, message = "{metadata_module.module_id.length_range}", groups = {Created.class, Updated.class})
    private String id;

    @Schema(description = "模块名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{metadata_module.name.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 255, message = "{metadata_module.name.length_range}", groups = {Created.class, Updated.class})
    private String name;

    @TableField("project_id")
    @Schema(description = "项目/工作空间ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{metadata_module.project_id.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 50, message = "{metadata_module.project_id.length_range}", groups = {Created.class, Updated.class})
    private String projectId;

    @TableField("parent_id")
    @Schema(description = "父节点ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{metadata_module.parent_id.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 50, message = "{metadata_module.parent_id.length_range}", groups = {Created.class, Updated.class})
    private String parentId;

    @TableField("module_type")
    @Schema(description = "目录类型: API/WORKFLOW/SQL/MIXED", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{metadata_module.module_type.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 20, message = "{metadata_module.module_type.length_range}", groups = {Created.class, Updated.class})
    private String moduleType;

    @TableField("type_id")
    @Schema(description = "根据module_type来存对应业务下第一层ID（如WORKFLOW类型存储workspace_id）")
    @Size(max = 50, message = "type_id长度不能超过50")
    private String typeId;

    @Schema(description = "排序值", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{metadata_module.pos.not_blank}", groups = {Created.class})
    private Long pos;

    @TableField("create_time")
    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{metadata_module.create_time.not_blank}", groups = {Created.class})
    private Long createTime;

    @TableField("update_time")
    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{metadata_module.update_time.not_blank}", groups = {Created.class})
    private Long updateTime;

    @TableField(value = "deleted_time", typeHandler = DateTimeTypeHandler.class)
    @Schema(description = "删除时间")
    private Long deletedTime;
}
