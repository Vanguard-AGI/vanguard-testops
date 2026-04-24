package io.vanguard.testops.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 测试报告列表顶部统计（总报告数、已完成、运行中、失败、平均成功率）
 */
@Data
public class TestReportStatsDTO {

    @Schema(description = "总报告数（当前筛选条件下）")
    private Long total;

    @Schema(description = "已完成数量")
    private Long completed;

    @Schema(description = "运行中数量")
    private Long running;

    @Schema(description = "失败数量")
    private Long failed;

    @Schema(description = "平均成功率（仅对已完成报告求平均，百分比 0-100）")
    private Double avgSuccessRate;
}
