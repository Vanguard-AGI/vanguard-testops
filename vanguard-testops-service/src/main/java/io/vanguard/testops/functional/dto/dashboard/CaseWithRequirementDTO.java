package io.vanguard.testops.functional.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用例信息DTO（含关联的需求信息）
 * 用于变更原因/阻塞原因的用例详情展示
 */
@Data
public class CaseWithRequirementDTO {
    
    @Schema(description = "用例ID")
    private String caseId;
    
    @Schema(description = "用例名称")
    private String caseName;
    
    @Schema(description = "用例编号")
    private Long caseNum;
    
    @Schema(description = "CS复杂度分值")
    private Double csScore;
    
    @Schema(description = "复杂度等级")
    private String complexityLevel;
    
    @Schema(description = "关联的需求ID（可能为null）")
    private String storyId;
    
    @Schema(description = "关联的需求名称（可能为null）")
    private String storyName;
    
    @Schema(description = "关联的测试计划ID（可能为null）")
    private String testPlanId;
    
    @Schema(description = "关联的测试计划名称（可能为null）")
    private String testPlanName;
    
    @Schema(description = "变更原因（如果是变更原因查询）")
    private String changeReason;
    
    @Schema(description = "阻塞原因（如果是阻塞原因查询）")
    private String blockReason;
    
    @Schema(description = "创建时间")
    private Long createTime;
}

