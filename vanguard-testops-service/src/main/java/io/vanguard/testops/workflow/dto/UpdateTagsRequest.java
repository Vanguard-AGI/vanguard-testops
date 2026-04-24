package io.vanguard.testops.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 更新标签请求
 */
@Data
public class UpdateTagsRequest {
    
    @Schema(description = "标签列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "标签列表不能为空")
    private List<String> tags;
}

