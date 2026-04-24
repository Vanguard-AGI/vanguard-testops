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
 * 工作流运行记录实体类
 */
@Data
@TableName(value = "workflow_run", autoResultMap = true)
public class WorkflowRun implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "run_id", type = IdType.ASSIGN_ID)
    @Schema(description = "运行ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "运行ID不能为空")
    @Size(min = 1, max = 50, message = "运行ID长度范围1-50")
    private String runId;

    @TableField("workflow_id")
    @Schema(description = "关联工作流ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "工作流ID不能为空")
    @Size(min = 1, max = 50, message = "工作流ID长度范围1-50")
    private String workflowId;

    @TableField("project_id")
    @Schema(description = "项目ID")
    @Size(max = 50, message = "项目ID长度不能超过50")
    private String projectId;

    @TableField("module_id")
    @Schema(description = "关联模块ID")
    @Size(max = 50, message = "模块ID长度不能超过50")
    private String moduleId;

    @TableField("workflow_name")
    @Schema(description = "工作流名称快照", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "工作流名称不能为空")
    @Size(max = 255, message = "工作流名称长度不能超过255")
    private String workflowName;

    @TableField("trigger_type")
    @Schema(description = "触发类型: MANUAL/SCHEDULE/API", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "触发类型不能为空")
    @Size(max = 20, message = "触发类型长度不能超过20")
    private String triggerType;

    @TableField("trigger_user")
    @Schema(description = "触发人")
    @Size(max = 50, message = "触发人长度不能超过50")
    private String triggerUser;

    @TableField("status")
    @Schema(description = "状态: PENDING/RUNNING/SUCCESS/FAILED/CANCELLED", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "状态不能为空")
    @Size(max = 20, message = "状态长度不能超过20")
    private String status;

    @TableField("start_time")
    @Schema(description = "开始时间（毫秒）")
    private Long startTime;

    @TableField("end_time")
    @Schema(description = "结束时间（毫秒）")
    private Long endTime;

    @TableField("duration_ms")
    @Schema(description = "总耗时（毫秒）")
    private Long durationMs;

    @TableField("total_steps")
    @Schema(description = "总步骤数")
    private Integer totalSteps;

    @TableField("passed_count")
    @Schema(description = "通过步骤数")
    private Integer passedCount;

    @TableField("failed_count")
    @Schema(description = "失败步骤数")
    private Integer failedCount;

    @TableField("skipped_count")
    @Schema(description = "跳过步骤数")
    private Integer skippedCount;

    @TableField("pending_count")
    @Schema(description = "待执行步骤数")
    private Integer pendingCount;

    @TableField(value = "result_summary", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "运行结果摘要（JSON）")
    private Map<String, Object> resultSummary;

    @TableField("environment_id")
    @Schema(description = "执行环境ID")
    @Size(max = 50, message = "环境ID长度不能超过50")
    private String environmentId;

    @TableField("environment_name")
    @Schema(description = "执行环境名称（快照）")
    @Size(max = 255, message = "环境名称长度不能超过255")
    private String environmentName;

    @TableField("report_id")
    @Schema(description = "关联的测试报告ID")
    @Size(max = 50, message = "报告ID长度不能超过50")
    private String reportId;

    @TableField("create_time")
    @Schema(description = "创建时间（毫秒）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long createTime;

    @TableField("update_time")
    @Schema(description = "最后更新时间（毫秒）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long updateTime;

    @TableField(value = "deleted_time", typeHandler = io.vanguard.testops.handler.DateTimeTypeHandler.class)
    @Schema(description = "删除时间（软删除）")
    private Long deletedTime;
}

