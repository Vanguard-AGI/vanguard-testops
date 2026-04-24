package io.vanguard.testops.requirementquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 原因分布项 - 详情接口返回（饼图用：名称 + 数量，前端据此算百分比）
 */
@Data
public class RequirementQualityReasonDistributionDTO {

    @Schema(description = "原因名称（展示用）")
    private String name;

    @Schema(description = "数量（该原因的用例/记录数）")
    private Long value;
}
