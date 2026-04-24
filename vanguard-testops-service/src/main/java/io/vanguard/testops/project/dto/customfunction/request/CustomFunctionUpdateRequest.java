package io.vanguard.testops.project.dto.customfunction.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author: Jan
 * @date: 2026-04-22
 * @version: 1.0
 */
@Data
public class CustomFunctionUpdateRequest extends CustomFunctionRequest {

    @Schema(description = "主键ID")
    @NotBlank(message = "{custom_function.id.not_blank}")
    @Size(min = 1, max = 50, message = "{custom_function.id.length_range}")
    private String id;
}
