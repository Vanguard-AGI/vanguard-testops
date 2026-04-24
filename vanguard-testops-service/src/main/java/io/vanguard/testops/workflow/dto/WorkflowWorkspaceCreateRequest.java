package io.vanguard.testops.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建工作空间请求
 */
@Data
public class WorkflowWorkspaceCreateRequest {
    
    @Schema(description = "工作空间名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "工作空间名称不能为空")
    @Size(max = 255, message = "工作空间名称长度不能超过255")
    private String name;
    
    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "项目ID不能为空")
    @Size(max = 50, message = "项目ID长度不能超过50")
    private String projectId;
    
    @Schema(description = "负责人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "负责人不能为空")
    @Size(max = 50, message = "负责人长度不能超过50")
    private String responsiblePerson;
    
    @Schema(description = "描述")
    @Size(max = 1000, message = "描述长度不能超过1000")
    private String description;
    
    @Schema(description = "图标（emoji）")
    @Size(max = 20, message = "图标长度不能超过20")
    private String icon;
    
    @Schema(description = "图标背景颜色（CSS类名）")
    @Size(max = 50, message = "图标颜色长度不能超过50")
    private String iconColor;
    
    @Schema(description = "可见范围: PRIVATE/PROJECT/ORG")
    @Size(max = 20, message = "可见范围长度不能超过20")
    private String visibility = "PRIVATE";
}

