package io.vanguard.testops.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 工作流定义详情 DTO
 * 用于返回工作流完整信息（包含节点和连线）
 */
@Data
public class WorkflowDefinitionDTO {

    @Schema(description = "工作流ID")
    private String workflowId;

    @Schema(description = "项目ID")
    private String projectId;

    @Schema(description = "模块ID")
    private String moduleId;

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

    @Schema(description = "全局变量定义")
    private Map<String, Object> globalVars;

    @Schema(description = "环境ID")
    private String environmentId;

    @Schema(description = "定时触发配置")
    private Map<String, Object> scheduleConfig;

    @Schema(description = "状态: DRAFT/PUBLISHED")
    private String status;

    // ⭐ 核心字段 - 节点列表（包含坐标）
    @Schema(description = "节点列表")
    private List<WorkflowNodeDTO> nodes;

    // ⭐ 核心字段 - 连线列表（包含样式）
    @Schema(description = "连线列表")
    private List<WorkflowConnectionDTO> connections;

    @Schema(description = "创建人")
    private String createUser;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "更新时间")
    private Long updateTime;

    @Schema(description = "更新人")
    private String updateUser;
}

