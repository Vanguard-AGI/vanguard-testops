package io.vanguard.testops.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "用户项目信息")
public class UserProjectInfoDTO {
    
    @Schema(description = "用户ID")
    private String userId;
    
    @Schema(description = "项目ID列表")
    private List<ProjectRequest> projects;
}

