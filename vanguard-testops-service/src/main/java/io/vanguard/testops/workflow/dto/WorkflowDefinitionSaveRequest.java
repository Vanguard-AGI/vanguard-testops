package io.vanguard.testops.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 工作流保存请求 DTO
 * 用于创建或更新工作流
 */
@Data
public class WorkflowDefinitionSaveRequest {

    @Schema(description = "工作流ID（有则更新，无则创建）")
    @Size(max = 50, message = "工作流ID长度不能超过50")
    private String workflowId;

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "项目ID不能为空")
    @Size(min = 1, max = 50, message = "项目ID长度范围1-50")
    private String projectId;

    @Schema(description = "模块ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "模块ID不能为空")
    @Size(min = 1, max = 50, message = "模块ID长度范围1-50")
    private String moduleId;

    @Schema(description = "工作流名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "工作流名称不能为空")
    @Size(min = 1, max = 255, message = "工作流名称长度范围1-255")
    private String name;

    @Schema(description = "描述")
    @Size(max = 1000, message = "描述长度不能超过1000")
    private String description;

    @Schema(description = "工作流分类: API/UI/AGENT", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "分类不能为空")
    @Size(max = 32, message = "分类长度不能超过32")
    private String category;

    @Schema(description = "类型: TEST_CASE(测试用例) / PUBLIC_STEP(公共步骤)")
    @Size(max = 20, message = "类型长度不能超过20")
    private String type;

    @Schema(description = "全局变量定义")
    private Map<String, Object> globalVars;

    @Schema(description = "环境ID")
    @Size(max = 50, message = "环境ID长度不能超过50")
    private String environmentId;

    // ⭐ 核心字段 - 节点列表（包含坐标）
    @Schema(description = "节点列表")
    private List<WorkflowNodeDTO> nodes;

    // ⭐ 核心字段 - 连线列表（包含样式）
    @Schema(description = "连线列表")
    private List<WorkflowConnectionDTO> connections;
}

