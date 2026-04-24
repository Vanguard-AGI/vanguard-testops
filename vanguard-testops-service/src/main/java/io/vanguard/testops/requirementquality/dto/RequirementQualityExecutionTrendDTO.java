package io.vanguard.testops.requirementquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用例执行趋势 - 详情接口返回（饼图/柱线图：日期、通过/失败/阻塞数、通过率）
 */
@Data
public class RequirementQualityExecutionTrendDTO {

    @Schema(description = "日期，格式 MM-dd，用于图表 X 轴")
    private String date;

    @Schema(description = "通过数")
    private Long passed;

    @Schema(description = "失败数")
    private Long failed;

    @Schema(description = "阻塞数")
    private Long blocked;

    @Schema(description = "通过率，0-100")
    private Double passRate;
}
