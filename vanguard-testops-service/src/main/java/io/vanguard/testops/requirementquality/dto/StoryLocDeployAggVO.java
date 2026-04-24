package io.vanguard.testops.requirementquality.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 按 story_id 从 requirement_change_stats 聚合得到的行数与发布指标（用于写回 meego_story_stats）
 */
@Data
public class StoryLocDeployAggVO {
    private Integer frontendLocChanged;
    private Integer backendLocChanged;
    private Integer deployTotalCount;
    private Integer deployFailureCount;
    private Long lastDeployTime;
    private BigDecimal changeFailureRate;
}
