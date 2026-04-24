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
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 工作流测试报告实体类
 */
@Data
@TableName(value = "workflow_test_report", autoResultMap = true)
public class WorkflowTestReport implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "report_id", type = IdType.ASSIGN_ID)
    @Schema(description = "报告ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "报告ID不能为空")
    @Size(min = 1, max = 50, message = "报告ID长度范围1-50")
    private String reportId;

    @TableField("project_id")
    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "项目ID不能为空")
    @Size(max = 50, message = "项目ID长度不能超过50")
    private String projectId;

    @TableField("report_name")
    @Schema(description = "报告名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "报告名称不能为空")
    @Size(max = 255, message = "报告名称长度不能超过255")
    private String reportName;

    @TableField("report_type")
    @Schema(description = "报告类型: MANUAL(手动生成)/AUTO(自动生成)/SCHEDULE(定时生成)")
    @Size(max = 20, message = "报告类型长度不能超过20")
    private String reportType = "MANUAL";

    @TableField(value = "tags", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "标签列表，如：[\"电商\", \"核心功能\", \"回归测试\"]")
    private List<String> tags;

    @TableField("executor")
    @Schema(description = "执行人/创建人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "执行人不能为空")
    @Size(max = 50, message = "执行人长度不能超过50")
    private String executor;

    @TableField("trigger_type")
    @Schema(description = "触发类型: MANUAL(手动)/SCHEDULE(定时)/API(接口触发)")
    @Size(max = 20, message = "触发类型长度不能超过20")
    private String triggerType = "MANUAL";

    @TableField("status")
    @Schema(description = "报告状态: RUNNING(运行中)/COMPLETED(已完成)/FAILED(失败)/CANCELLED(已取消)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "状态不能为空")
    @Size(max = 20, message = "状态长度不能超过20")
    private String status = "RUNNING";

    @TableField("start_time")
    @Schema(description = "开始时间（毫秒）")
    private Long startTime;

    @TableField("end_time")
    @Schema(description = "结束时间（毫秒）")
    private Long endTime;

    @TableField("duration_ms")
    @Schema(description = "报告生成耗时（毫秒）：从reportId生成到所有workflow完成的时间差")
    private Long durationMs;

    @TableField("execution_duration_ms")
    @Schema(description = "执行时长（毫秒）：所有workflow执行耗时的总和")
    private Long executionDurationMs;

    @TableField("total_workflows")
    @Schema(description = "包含的工作流数量")
    private Integer totalWorkflows = 0;

    @TableField("total_tests")
    @Schema(description = "总测试数（所有工作流的步骤总数）")
    private Integer totalTests = 0;

    @TableField("success_tests")
    @Schema(description = "成功测试数")
    private Integer successTests = 0;

    @TableField("failed_tests")
    @Schema(description = "失败测试数")
    private Integer failedTests = 0;

    @TableField("skipped_tests")
    @Schema(description = "跳过测试数")
    private Integer skippedTests = 0;

    @TableField("pending_tests")
    @Schema(description = "待执行测试数")
    private Integer pendingTests = 0;

    @TableField("success_rate")
    @Schema(description = "成功率（百分比，如：92.30）")
    private BigDecimal successRate;

    @TableField("avg_duration_seconds")
    @Schema(description = "平均执行时长（秒）")
    private Integer avgDurationSeconds;

    @TableField("summary")
    @Schema(description = "报告摘要/描述")
    private String summary;

    @TableField(value = "result_summary", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "详细结果摘要（JSON格式，包含各工作流的执行情况）")
    private Map<String, Object> resultSummary;

    @TableField("environment_id")
    @Schema(description = "执行环境ID")
    @Size(max = 50, message = "环境ID长度不能超过50")
    private String environmentId;

    @TableField("environment_name")
    @Schema(description = "执行环境名称（快照）")
    @Size(max = 255, message = "环境名称长度不能超过255")
    private String environmentName;

    @TableField("report_file_id")
    @Schema(description = "报告文件ID（关联 metadata_file_resource）")
    @Size(max = 50, message = "报告文件ID长度不能超过50")
    private String reportFileId;

    @TableField("create_time")
    @Schema(description = "创建时间（毫秒）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long createTime;

    @TableField("create_user")
    @Schema(description = "创建人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "创建人不能为空")
    @Size(max = 50, message = "创建人长度不能超过50")
    private String createUser;

    @TableField("update_time")
    @Schema(description = "最后更新时间（毫秒）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long updateTime;

    @TableField("update_user")
    @Schema(description = "最后更新人")
    @Size(max = 50, message = "更新人长度不能超过50")
    private String updateUser;

    @TableField(value = "deleted_time", typeHandler = io.vanguard.testops.handler.DateTimeTypeHandler.class)
    @Schema(description = "删除时间（软删除，NULL表示未删除）")
    private Long deletedTime;

    @TableField("deleted_by")
    @Schema(description = "删除人")
    @Size(max = 50, message = "删除人长度不能超过50")
    private String deletedBy;
}

