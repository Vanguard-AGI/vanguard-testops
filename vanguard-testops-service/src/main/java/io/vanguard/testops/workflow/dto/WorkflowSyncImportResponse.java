package io.vanguard.testops.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 工作流同步导入响应 DTO
 */
@Data
public class WorkflowSyncImportResponse {

    @Schema(description = "工作流ID")
    private String workflowId;

    @Schema(description = "工作流名称")
    private String workflowName;

    @Schema(description = "工作流同步模块ID")
    private String moduleId;

    @Schema(description = "节点总数")
    private Integer nodeCount;

    @Schema(description = "成功导入的节点数")
    private Integer successCount;

    @Schema(description = "导入失败的节点数")
    private Integer failCount;
}

