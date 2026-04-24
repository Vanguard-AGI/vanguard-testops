package io.vanguard.testops.functional.request;

import io.vanguard.testops.functional.dto.BaseFunctionalCaseBatchDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author Jan
 */
@Data
public class FunctionalCaseBatchRequest extends BaseFunctionalCaseBatchDTO {


    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{functional_case.project_id.not_blank}")
    private String projectId;

    @Schema(description = "删除列表版本/删除全部版本")
    private Boolean deleteAll = true;

}
