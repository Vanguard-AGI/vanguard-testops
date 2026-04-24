package io.vanguard.testops.requirementquality.dto;

import lombok.Data;

/**
 * 用例执行趋势 - Mapper 查询结果（按日统计通过/失败/阻塞数）
 */
@Data
public class RequirementQualityExecutionTrendVO {

    /** 日期，格式 yyyy-MM-dd */
    private String execDate;
    private Long passed;
    private Long failed;
    private Long blocked;
}
