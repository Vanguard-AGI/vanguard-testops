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
@TableName(value = "script_manage", autoResultMap = true)
public class ScriptManage implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "script_id", type = IdType.ASSIGN_ID)
    @Schema(description = "脚本主键ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{script_manage.script_id.not_blank}", groups = {Updated.class})
    @Size(min = 1, max = 64, message = "{script_manage.script_id.length_range}", groups = {Created.class, Updated.class})
    private String scriptId;

    @TableField("script_name")
    @Schema(description = "脚本名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{script_manage.script_name.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 255, message = "{script_manage.script_name.length_range}", groups = {Created.class, Updated.class})
    private String scriptName;

    @TableField("script_type")
    @Schema(description = "脚本类型: PYTHON/SQL/SHELL/JAVA", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{script_manage.script_type.not_blank}", groups = {Created.class})
    @Size(min = 1, max = 20, message = "{script_manage.script_type.length_range}", groups = {Created.class, Updated.class})
    private String scriptType;

    @TableField("script_content")
    @Schema(description = "脚本内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{script_manage.script_content.not_blank}", groups = {Created.class})
    private String scriptContent;

    @TableField("create_time")
    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{script_manage.create_time.not_blank}", groups = {Created.class})
    private Long createTime;

    @TableField("update_time")
    @Schema(description = "更新时间")
    private Long updateTime;

    @TableField(value = "deleted_time", typeHandler = DateTimeTypeHandler.class)
    @Schema(description = "删除时间")
    private Long deletedTime;
}

