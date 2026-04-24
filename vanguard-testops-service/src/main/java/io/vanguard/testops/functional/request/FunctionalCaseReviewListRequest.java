package io.vanguard.testops.functional.request;

import io.vanguard.testops.system.dto.sdk.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FunctionalCaseReviewListRequest extends BasePageRequest {
    @Schema(description = "功能用例id")
    private String caseId;

}
