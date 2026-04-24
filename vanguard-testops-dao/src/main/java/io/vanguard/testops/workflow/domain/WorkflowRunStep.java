package io.vanguard.testops.workflow.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 工作流步骤执行明细实体类
 */
@Data
@TableName(value = "workflow_run_step", autoResultMap = true)
public class WorkflowRunStep implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "run_step_id", type = IdType.ASSIGN_ID)
    @Schema(description = "运行步骤ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "运行步骤ID不能为空")
    @Size(min = 1, max = 50, message = "运行步骤ID长度范围1-50")
    private String runStepId;

    @TableField("run_id")
    @Schema(description = "运行ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "运行ID不能为空")
    @Size(min = 1, max = 50, message = "运行ID长度范围1-50")
    private String runId;

    @TableField("step_id")
    @Schema(description = "步骤ID（关联 workflow_step）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "步骤ID不能为空")
    @Size(min = 1, max = 50, message = "步骤ID长度范围1-50")
    private String stepId;

    @TableField("step_name")
    @Schema(description = "步骤名称快照", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "步骤名称不能为空")
    @Size(max = 255, message = "步骤名称长度不能超过255")
    private String stepName;

    @TableField("step_type")
    @Schema(description = "步骤类型快照")
    @Size(max = 20, message = "步骤类型长度不能超过20")
    private String stepType;

    @TableField("order_num")
    @Schema(description = "步骤顺序（快照）")
    private Integer orderNum;

    @TableField("status")
    @Schema(description = "状态: PENDING/RUNNING/SUCCESS/FAILED/SKIPPED", requiredMode = Schema.RequiredMode.REQUIRED)
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
    @Schema(description = "耗时（毫秒）")
    private Long durationMs;

    @TableField(value = "request_data", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "实际请求内容（JSON）")
    private Map<String, Object> requestData;

    @TableField(value = "response_data", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "实际响应内容（JSON）")
    private Map<String, Object> responseData;

    @TableField(value = "assertion_logs", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "断言验证日志（JSON数组）")
    @JsonProperty("assertion")
    private List<Map<String, Object>> assertion;

    @TableField(value = "extract_vars", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "提取的变量（JSON对象）")
    private Map<String, Object> extractVars;

    @TableField("error_msg")
    @Schema(description = "错误消息")
    private String errorMsg;

    @TableField("error_stack")
    @Schema(description = "错误堆栈")
    private String errorStack;

    @TableField("description")
    @Schema(description = "执行描述/摘要")
    private String description;

    @TableField("create_time")
    @Schema(description = "创建时间（毫秒）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long createTime;

    @TableField("update_time")
    @Schema(description = "最后更新时间（毫秒）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long updateTime;
}

