package io.vanguard.testops.functional.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class TrackModificationTimeRequest {
    @Schema(description = "测试计划ID（可选，已废弃，修改耗时记录在用例维度）")
    private String testPlanId;
    
    @Schema(description = "用例ID（必填）")
    private String caseId;
    
    @Schema(description = "修改耗时(毫秒)")
    private Long modificationCostMs;
}
