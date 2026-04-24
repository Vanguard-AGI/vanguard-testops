package io.vanguard.testops.system.controller.param;

import io.vanguard.testops.sdk.constants.UserRoleType;
import io.vanguard.testops.sdk.valid.EnumValue;
import io.vanguard.testops.validation.groups.Created;
import io.vanguard.testops.validation.groups.Updated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * UserRoleUpdateRequest 约束定义
 * @author Jan
 */
@Data
public class UserRoleUpdateRequestDefinition {
    @NotBlank(groups = {Updated.class})
    @Size(min = 1, max = 50, groups = {Created.class, Updated.class})
    private String id;
    
    @NotBlank(groups = {Created.class})
    @Size(min = 1, max = 255, groups = {Created.class, Updated.class})
    private String name;

    @NotBlank(groups = {Created.class})
    @EnumValue(enumClass = UserRoleType.class, groups = {Created.class, Updated.class})
    @Size(min = 1, max = 20, groups = {Created.class, Updated.class})
    private String type;
}
