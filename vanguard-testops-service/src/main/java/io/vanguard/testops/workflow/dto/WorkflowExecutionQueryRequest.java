package io.vanguard.testops.workflow.dto;

import io.vanguard.testops.system.dto.sdk.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作流执行记录分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class WorkflowExecutionQueryRequest extends BasePageRequest {
    
    @Schema(description = "报告ID（必需）")
    private String reportId;
    
    @Schema(description = "状态筛选: RUNNING/SUCCESS/FAILED/CANCELLED")
    private String status;
    
    @Schema(description = "关键词搜索（工作流名称）")
    private String keyword;
}

