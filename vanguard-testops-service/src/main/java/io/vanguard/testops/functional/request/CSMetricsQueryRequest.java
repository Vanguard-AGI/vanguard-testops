package io.vanguard.testops.functional.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * CS指标查询请求
 */
@Data
public class CSMetricsQueryRequest {

    @Schema(description = "统计维度：project-项目, user-用户, team-团队", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "统计维度不能为空")
    private String dimension;

    @Schema(description = "维度值（项目ID/用户ID/团队ID）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "维度值不能为空")
    private String dimensionValue;

    @Schema(description = "开始时间（毫秒时间戳）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long startTime;

    @Schema(description = "结束时间（毫秒时间戳）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long endTime;
}

