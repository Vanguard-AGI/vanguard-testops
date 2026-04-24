package io.vanguard.testops.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 项目简单信息DTO（用于公开接口）
 */
@Data
public class ProjectSimpleDTO {
    
    @Schema(description = "项目ID")
    private String id;
    
    @Schema(description = "项目名称")
    private String name;
}

