package io.vanguard.testops.system.controller.param;

import io.vanguard.testops.validation.groups.Created;
import io.vanguard.testops.validation.groups.Updated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * @author Jan
 */
@Data
public class GlobalUserRoleRelationUpdateRequestDefinition {

    @NotEmpty(groups = {Created.class, Updated.class})
    private List<
            @NotBlank(groups = {Created.class, Updated.class})
            @Size(groups = {Created.class, Updated.class})
            String> userIds;

    @NotBlank(groups = {Created.class})
    @Size(min = 1, max = 50, groups = {Created.class, Updated.class})
    private String roleId;
}
