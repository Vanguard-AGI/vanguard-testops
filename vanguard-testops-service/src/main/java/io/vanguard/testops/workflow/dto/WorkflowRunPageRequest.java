package io.vanguard.testops.workflow.dto;

import io.vanguard.testops.system.dto.sdk.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作流运行记录分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class WorkflowRunPageRequest extends BasePageRequest {
    
    @Schema(description = "工作流ID")
    private String workflowId;
    
    @Schema(description = "项目ID")
    private String projectId;
    
    @Schema(description = "状态筛选")
    private String status;
    
    @Schema(description = "触发类型筛选")
    private String triggerType;
    
    @Schema(description = "触发用户ID（用于个人维度隔离）")
    private String triggerUser;
    
    @Schema(description = "关键词搜索（工作流名称）")
    private String keyword;
}

