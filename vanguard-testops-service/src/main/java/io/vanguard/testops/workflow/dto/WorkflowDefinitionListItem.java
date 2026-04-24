package io.vanguard.testops.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 工作流列表项 DTO
 * 用于分页查询结果
 */
@Data
public class WorkflowDefinitionListItem {

    @Schema(description = "工作流ID")
    private String workflowId;

    @Schema(description = "工作流名称")
    private String name;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "工作流分类: API/UI/AGENT")
    private String category;

    @Schema(description = "类型: TEST_CASE / PUBLIC_STEP")
    private String type;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "状态: DRAFT/PUBLISHED")
    private String status;

    @Schema(description = "步骤数量")
    private Integer stepCount;

    @Schema(description = "创建人")
    private String createUser;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "更新时间")
    private Long updateTime;

    @Schema(description = "最新执行时长（毫秒）")
    private Long lastDurationMs;

    @Schema(description = "最新执行状态: SUCCESS/FAILED/PENDING/RUNNING")
    private String lastRunStatus;

    @Schema(description = "最新执行时间")
    private Long lastRunTime;
}

