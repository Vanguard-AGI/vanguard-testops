package io.vanguard.testops.requirementquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 需求质量详情 - 用例执行明细行（排名、用例名、失败/成功次数、耗时、失败率等）
 */
@Data
public class RequirementQualityCaseExecutionRowDTO {

    @Schema(description = "测试计划ID")
    private String planId;

    @Schema(description = "用例ID")
    private String caseId;

    @Schema(description = "用例名称")
    private String name;

    @Schema(description = "失败次数")
    private Integer failCount;

    @Schema(description = "成功次数")
    private Integer successCount;

    @Schema(description = "执行次数")
    private Integer execCount;

    @Schema(description = "平均耗时(秒)")
    private Double avgTimeSeconds;

    @Schema(description = "最大耗时(秒)，无则与平均一致或0")
    private Double maxTimeSeconds;

    @Schema(description = "失败率，0-100")
    private Double failRate;

    @Schema(description = "成功率，0-100")
    private Double successRate;

    @Schema(description = "失败原因简述")
    private String failReason;
}
