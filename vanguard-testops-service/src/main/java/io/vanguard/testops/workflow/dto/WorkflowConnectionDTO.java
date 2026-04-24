package io.vanguard.testops.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 工作流连线 DTO
 * 对应前端的 Connection 接口
 */
@Data
public class WorkflowConnectionDTO {

    @Schema(description = "源节点ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String from;

    @Schema(description = "目标节点ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String to;

    // ⭐ 连线样式 - 前端关键字段
    @Schema(description = "连线标签（如：true/false/case1等）")
    private String label;

    @Schema(description = "连线颜色（如：#10B981/#EF4444）")
    private String color;

    // 可选字段
    @Schema(description = "流转条件表达式")
    private String conditionExpr;

    @Schema(description = "连线顺序")
    private Integer orderNum;
}

