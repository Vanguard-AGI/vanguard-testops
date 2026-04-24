package io.vanguard.testops.functional.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 需求信息DTO（用于效能大屏需求筛选）
 */
@Data
public class RequirementDTO {
    
    @Schema(description = "需求ID (Story ID)")
    private String storyId;
    
    @Schema(description = "需求名称 (Story Name)")
    private String storyName;
    
    @Schema(description = "关联测试计划数")
    private Integer relatedTestPlanCount;
    
    @Schema(description = "关联用例数（去重）")
    private Integer relatedCaseCount;
    
    @Schema(description = "缺陷数")
    private Integer defectCount;
    
    @Schema(description = "测分编写预期时间（人天）")
    private Double testAnalysisTime;
}

