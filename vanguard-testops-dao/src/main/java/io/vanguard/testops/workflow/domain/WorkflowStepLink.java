package io.vanguard.testops.workflow.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工作流步骤连线实体类
 * 对应前端的 Connection
 */
@Data
@TableName(value = "workflow_step_link", autoResultMap = true)
public class WorkflowStepLink implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "link_id", type = IdType.ASSIGN_ID)
    @Schema(description = "连线ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "连线ID不能为空")
    @Size(min = 1, max = 50, message = "连线ID长度范围1-50")
    private String linkId;

    @TableField("workflow_id")
    @Schema(description = "工作流ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "工作流ID不能为空")
    @Size(min = 1, max = 50, message = "工作流ID长度范围1-50")
    private String workflowId;

    @TableField("source_step_id")
    @Schema(description = "源步骤ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "源步骤ID不能为空")
    @Size(min = 1, max = 50, message = "源步骤ID长度范围1-50")
    private String sourceStepId;

    @TableField("target_step_id")
    @Schema(description = "目标步骤ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "目标步骤ID不能为空")
    @Size(min = 1, max = 50, message = "目标步骤ID长度范围1-50")
    private String targetStepId;

    // ⭐ 连线样式 - 前端需要的关键字段
    @Schema(description = "连线标签（如：true/false/case1等，用于条件分支）")
    @Size(max = 100, message = "标签长度不能超过100")
    private String label;

    @Schema(description = "连线颜色（如：#10B981/#EF4444，用于区分不同分支）")
    @Size(max = 20, message = "颜色长度不能超过20")
    private String color;

    @TableField("condition_expr")
    @Schema(description = "流转条件表达式（当label不足以表达时使用）")
    @Size(max = 500, message = "条件表达式长度不能超过500")
    private String conditionExpr;

    @TableField("order_num")
    @Schema(description = "连线顺序（同一源步骤的多条连线排序）")
    private Integer orderNum;

    @TableField("create_time")
    @Schema(description = "创建时间（毫秒）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long createTime;

    @TableField("update_time")
    @Schema(description = "最后修改时间（毫秒）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long updateTime;

    @TableField("deleted_time")
    @Schema(description = "删除时间（软删除，NULL表示未删除）")
    private LocalDateTime deletedTime;
}

