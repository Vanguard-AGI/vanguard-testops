package io.vanguard.testops.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 工作流工作空间 DTO（用于详情返回，包含统计信息）
 */
@Data
public class WorkflowWorkspaceDTO {
    
    @Schema(description = "工作空间ID")
    private String workspaceId;
    
    @Schema(description = "工作空间名称")
    private String workspaceName;
    
    @Schema(description = "项目ID")
    private String projectId;
    
    @Schema(description = "负责人")
    private String owner;
    
    @Schema(description = "负责人名称")
    private String ownerName;
    
    @Schema(description = "全局变量")
    private Map<String, Object> globalVars;
    
    @Schema(description = "可见范围")
    private String visibility;
    
    @Schema(description = "描述")
    private String description;
    
    @Schema(description = "图标")
    private String icon;
    
    @Schema(description = "图标背景颜色")
    private String iconColor;
    
    // 统计字段
    @Schema(description = "测试用例数")
    private Integer testCaseCount;
    
    @Schema(description = "模块数")
    private Integer moduleCount;
    
    @Schema(description = "成员数")
    private Integer memberCount;
    
    @Schema(description = "通过率")
    private Double passRate;
    
    @Schema(description = "状态: running/failed/not-run")
    private String status;
    
    @Schema(description = "最后运行时间")
    private String lastRun;
    
    @Schema(description = "创建时间")
    private Long createTime;
    
    @Schema(description = "更新时间")
    private Long updateTime;
    
    @Schema(description = "工作流同步模块ID")
    private String syncModuleId;
}

