package io.vanguard.testops.system.controller.param;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * @author : Jan
 */
@Getter
@Setter
public class GlobalUserRoleRelationQueryRequestDefinition {
    @NotBlank
    private String roleId;
}
