package io.vanguard.testops.workflow.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 工作流步骤实体类
 * 对应前端的 WorkflowNode
 */
@Data
@TableName(value = "workflow_step", autoResultMap = true)
public class WorkflowStep implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "step_id", type = IdType.ASSIGN_ID)
    @Schema(description = "步骤ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "步骤ID不能为空")
    @Size(min = 1, max = 50, message = "步骤ID长度范围1-50")
    private String stepId;

    @TableField("workflow_id")
    @Schema(description = "所属工作流ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "工作流ID不能为空")
    @Size(min = 1, max = 50, message = "工作流ID长度范围1-50")
    private String workflowId;

    @Schema(description = "步骤名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "步骤名称不能为空")
    @Size(min = 1, max = 255, message = "步骤名称长度范围1-255")
    private String name;

    @TableField("step_type")
    @Schema(description = "类型: API/SQL/WAIT/IF/LOOP/SCRIPT/WEBSOCKET/DUBBO/CONDITION", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "步骤类型不能为空")
    @Size(min = 1, max = 20, message = "步骤类型长度范围1-20")
    private String stepType;

    @TableField("order_num")
    @Schema(description = "列表模式下的顺序", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "顺序不能为空")
    private Long orderNum;

    @TableField(value = "step_config", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "步骤核心配置（API请求/断言/提取变量等）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Object> stepConfig;

    // ⭐ 画布坐标 - 前端需要的关键字段
    @TableField("position_x")
    @Schema(description = "节点在画布上的X坐标（像素）")
    private BigDecimal positionX;

    @TableField("position_y")
    @Schema(description = "节点在画布上的Y坐标（像素）")
    private BigDecimal positionY;

    // 引用模式
    @TableField("ref_mode")
    @Schema(description = "引用模式: NONE/COPY/REF_METADATA/REF_WORKFLOW")
    @Size(max = 20, message = "引用模式长度不能超过20")
    private String refMode;

    @TableField("ref_metadata_id")
    @Schema(description = "关联的元数据ID(可选)")
    @Size(max = 50, message = "元数据ID长度不能超过50")
    private String refMetadataId;

    @TableField("ref_workflow_id")
    @Schema(description = "关联的workflow(可选)")
    @Size(max = 50, message = "工作流ID长度不能超过50")
    private String refWorkflowId;

    @Schema(description = "是否启用")
    private Boolean enable;

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

