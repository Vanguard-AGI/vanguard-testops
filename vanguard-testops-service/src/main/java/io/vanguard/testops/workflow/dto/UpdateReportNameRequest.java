package io.vanguard.testops.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新报告名称请求
 */
@Data
@Schema(description = "更新报告名称请求")
public class UpdateReportNameRequest {

    @Schema(description = "报告名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "报告名称不能为空")
    @Size(max = 255, message = "报告名称长度不能超过255")
    private String reportName;
}

