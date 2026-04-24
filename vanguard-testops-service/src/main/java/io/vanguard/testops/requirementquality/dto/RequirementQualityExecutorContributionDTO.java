package io.vanguard.testops.requirementquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 执行人贡献度 - 详情接口返回（含执行人姓名与用例数）
 */
@Data
public class RequirementQualityExecutorContributionDTO {

    @Schema(description = "执行人ID")
    private String executorId;

    @Schema(description = "执行人姓名")
    private String executorName;

    @Schema(description = "执行的用例数（该需求下该执行人执行过的去重用例数）")
    private Long caseCount;
}
