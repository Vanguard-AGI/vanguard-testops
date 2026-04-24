package io.vanguard.testops.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新工作空间请求
 */
@Data
public class WorkflowWorkspaceUpdateRequest {
    
    @Schema(description = "工作空间ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "工作空间ID不能为空")
    @Size(max = 50, message = "工作空间ID长度不能超过50")
    private String id;
    
    @Schema(description = "工作空间名称")
    @Size(max = 255, message = "工作空间名称长度不能超过255")
    private String name;
    
    @Schema(description = "负责人")
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
    private String visibility;
}

