package io.vanguard.testops.requirementquality.dto;

import lombok.Data;

/**
 * 原因分布项 - Mapper 查询结果（阻塞原因或变更原因统计）
 */
@Data
public class RequirementQualityReasonDistributionVO {

    /** 原因代码，如 ENVIRONMENT、REQUIREMENT_TEMP */
    private String reasonCode;
    /** 该原因的数量（用例数或记录数） */
    private Long count;
}
