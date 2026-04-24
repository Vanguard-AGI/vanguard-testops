package io.vanguard.testops.workflow.dto;

import io.vanguard.testops.system.dto.sdk.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作流分页查询请求 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkflowDefinitionPageRequest extends BasePageRequest {

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "项目ID不能为空")
    @Size(min = 1, max = 50, message = "项目ID长度范围1-50")
    private String projectId;

    @Schema(description = "模块ID（可选）")
    @Size(max = 50, message = "模块ID长度不能超过50")
    private String moduleId;

    @Schema(description = "工作空间ID（可选，用于查询该空间下的所有工作流）")
    @Size(max = 50, message = "工作空间ID长度不能超过50")
    private String workspaceId;

    @Schema(description = "搜索关键词（可选）")
    @Size(max = 100, message = "关键词长度不能超过100")
    private String keyword;

    @Schema(description = "状态筛选（可选）: DRAFT/PUBLISHED")
    @Size(max = 20, message = "状态长度不能超过20")
    private String status;

    @Schema(description = "分类筛选（可选）: API/UI/AGENT")
    @Size(max = 32, message = "分类长度不能超过32")
    private String category;
}

