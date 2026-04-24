package io.vanguard.testops.system.dto.request;

import io.vanguard.testops.system.dto.sdk.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Jan
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class OrganizationRequest extends BasePageRequest {

    @Schema(description =  "组织ID")
    private String organizationId;
}
