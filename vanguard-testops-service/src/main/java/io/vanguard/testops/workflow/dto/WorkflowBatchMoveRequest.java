package io.vanguard.testops.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 工作流批量移动请求 DTO
 */
@Data
@Schema(description = "工作流批量移动请求")
public class WorkflowBatchMoveRequest {

    @Schema(description = "工作流ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "工作流ID列表不能为空")
    private List<@Size(max = 50, message = "工作流ID长度不能超过50") String> workflowIds;

    @Schema(description = "目标模块ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "目标模块ID不能为空")
    @Size(min = 1, max = 50, message = "模块ID长度范围1-50")
    private String targetModuleId;
}

