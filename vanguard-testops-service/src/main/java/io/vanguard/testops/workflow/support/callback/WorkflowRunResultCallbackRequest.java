package io.vanguard.testops.workflow.support.callback;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 工作流执行结果回调请求
 * 执行机执行完成后调用此接口返回结果
 */
@Data
@Schema(description = "工作流执行结果回调请求")
public class WorkflowRunResultCallbackRequest {

    @Schema(description = "运行ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "运行ID不能为空")
    private String runId;

    @Schema(description = "状态: PENDING/RUNNING/SUCCESS/FAILED/CANCELLED", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "状态不能为空")
    private String status;

    @Schema(description = "开始时间（毫秒）")
    private Long startTime;

    @Schema(description = "结束时间（毫秒）")
    private Long endTime;

    @Schema(description = "总耗时（毫秒）")
    private Long durationMs;

    @Schema(description = "总步骤数")
    private Integer totalSteps;

    @Schema(description = "通过步骤数")
    private Integer passedCount;

    @Schema(description = "失败步骤数")
    private Integer failedCount;

    @Schema(description = "跳过步骤数")
    private Integer skippedCount;

    @Schema(description = "待执行步骤数")
    private Integer pendingCount;

    @Schema(description = "运行结果摘要（JSON）")
    private Map<String, Object> resultSummary;

    @Schema(description = "执行环境名称（快照）")
    private String environmentName;

    @Schema(description = "步骤执行明细列表")
    private List<WorkflowRunStepResult> steps;

    @Schema(description = "运行日志列表")
    private List<WorkflowRunLogResult> logs;

    @Schema(description = "批次索引（从1开始），如果为null或1，表示第一批或非分批模式")
    private Integer batchIndex;

    @Schema(description = "总批次数，如果为null或1，表示非分批模式")
    private Integer totalBatches;

    @Schema(description = "是否第一批（包含基础信息），如果为null，根据batchIndex判断")
    private Boolean isFirstBatch;

    @Schema(description = "是否最后一批（所有批次发送完成），如果为null，根据batchIndex和totalBatches判断")
    private Boolean isLastBatch;

    @Data
    @Schema(description = "步骤执行结果")
    public static class WorkflowRunStepResult {

        @Schema(description = "步骤ID（关联 workflow_step）", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "步骤ID不能为空")
        private String stepId;

        @Schema(description = "步骤名称快照", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "步骤名称不能为空")
        private String stepName;

        @Schema(description = "步骤类型快照")
        private String stepType;

        @Schema(description = "步骤顺序（快照）")
        private Integer orderNum;

        @Schema(description = "状态: PENDING/RUNNING/SUCCESS/FAILED/SKIPPED", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "状态不能为空")
        private String status;

        @Schema(description = "开始时间（毫秒）")
        private Long startTime;

        @Schema(description = "结束时间（毫秒）")
        private Long endTime;

        @Schema(description = "耗时（毫秒）")
        private Long durationMs;

        @Schema(description = "实际请求内容（JSON，可能是对象或数组）")
        private Object requestData;

        @Schema(description = "实际响应内容（JSON，可能是对象或数组）")
        private Object responseData;

        @Schema(description = "断言验证日志（JSON数组）")
        @JsonProperty("assertion")
        private List<Map<String, Object>> assertion;

        @Schema(description = "提取的变量（JSON对象，可能是对象或数组）")
        private Object extractVars;

        @Schema(description = "错误消息")
        private String errorMsg;

        @Schema(description = "错误堆栈")
        private String errorStack;

        @Schema(description = "执行描述/摘要")
        private String description;
    }

    @Data
    @Schema(description = "运行日志结果")
    public static class WorkflowRunLogResult {

        @Schema(description = "运行步骤ID（可选，如果日志属于某个步骤）")
        private String runStepId;

        @Schema(description = "步骤ID（可选，冗余字段）")
        private String stepId;

        @Schema(description = "日志级别: DEBUG/INFO/WARN/ERROR", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "日志级别不能为空")
        private String level;

        @Schema(description = "日志内容", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "日志内容不能为空")
        private String content;

        @Schema(description = "日志时间（毫秒）", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long logTime;
    }
}
