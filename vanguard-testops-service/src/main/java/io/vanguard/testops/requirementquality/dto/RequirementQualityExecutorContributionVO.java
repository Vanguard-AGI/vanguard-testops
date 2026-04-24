package io.vanguard.testops.requirementquality.dto;

import lombok.Data;

/**
 * 执行人贡献度 - Mapper 查询结果（按 story 下执行记录聚合）
 */
@Data
public class RequirementQualityExecutorContributionVO {

    private String executorId;
    private Long caseCount;
}
