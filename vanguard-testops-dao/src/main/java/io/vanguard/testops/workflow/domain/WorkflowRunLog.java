package io.vanguard.testops.workflow.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 工作流运行日志实体类
 */
@Data
@TableName("workflow_run_log")
public class WorkflowRunLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "log_id", type = IdType.AUTO)
    @Schema(description = "日志ID（自增主键）")
    private Long logId;

    @TableField("run_id")
    @Schema(description = "运行ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "运行ID不能为空")
    @Size(min = 1, max = 50, message = "运行ID长度范围1-50")
    private String runId;

    @TableField("run_step_id")
    @Schema(description = "运行步骤ID（可选）")
    @Size(max = 50, message = "运行步骤ID长度不能超过50")
    private String runStepId;

    @TableField("step_id")
    @Schema(description = "步骤ID（可选，冗余字段）")
    @Size(max = 50, message = "步骤ID长度不能超过50")
    private String stepId;

    @TableField("level")
    @Schema(description = "日志级别: DEBUG/INFO/WARN/ERROR", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "日志级别不能为空")
    @Size(max = 10, message = "日志级别长度不能超过10")
    private String level;

    @TableField("content")
    @Schema(description = "日志内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "日志内容不能为空")
    private String content;

    @TableField("log_time")
    @Schema(description = "日志时间（毫秒）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long logTime;

    @TableField("create_time")
    @Schema(description = "创建时间（毫秒）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long createTime;
}

