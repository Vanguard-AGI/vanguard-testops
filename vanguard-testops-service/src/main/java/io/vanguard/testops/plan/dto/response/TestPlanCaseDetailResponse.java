package io.vanguard.testops.plan.dto.response;

import io.vanguard.testops.functional.dto.FunctionalCaseDetailDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author Jan
 */
@Data
public class TestPlanCaseDetailResponse extends FunctionalCaseDetailDTO {

    @Schema(description = "用例缺陷列表总数量")
    private Integer bugListCount;

    @Schema(description = "用例执行历史总数量")
    private Integer runListCount;
}
