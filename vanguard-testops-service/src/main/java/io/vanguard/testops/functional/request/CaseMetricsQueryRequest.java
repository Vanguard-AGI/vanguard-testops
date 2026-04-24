package io.vanguard.testops.functional.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用例指标查询请求
 */
@Data
public class CaseMetricsQueryRequest {

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{case_metrics.project_id.not_blank}")
    private String projectId;

    @Schema(description = "指标级别：PROJECT-项目, PLAN-计划, USER-用户")
    private String metricLevel;

    @Schema(description = "时间维度：DAY-日, WEEK-周, MONTH-月, QUARTER-季, YEAR-年")
    private String timeDimension;

    @Schema(description = "开始时间（毫秒时间戳）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long startTime;

    @Schema(description = "结束时间（毫秒时间戳）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long endTime;

    @Schema(description = "用户ID（按人统计时填写）")
    private String userId;
}

