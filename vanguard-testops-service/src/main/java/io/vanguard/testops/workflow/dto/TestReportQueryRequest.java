package io.vanguard.testops.workflow.dto;

import io.vanguard.testops.system.dto.sdk.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 测试报告列表分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class TestReportQueryRequest extends BasePageRequest {
    
    @Schema(description = "项目ID")
    private String projectId;
    
    @Schema(description = "报告状态筛选: RUNNING/COMPLETED/FAILED/CANCELLED")
    private String status;
    
    @Schema(description = "报告类型筛选: MANUAL/AUTO/SCHEDULE")
    private String reportType;
    
    @Schema(description = "关键词搜索（报告名称/报告ID/执行人）")
    private String keyword;
    
    @Schema(description = "开始时间（毫秒）")
    private Long startTime;
    
    @Schema(description = "结束时间（毫秒）")
    private Long endTime;
}

