package io.vanguard.testops.system.controller.param;

import io.vanguard.testops.sdk.constants.CustomFieldType;
import io.vanguard.testops.sdk.valid.EnumValue;
import io.vanguard.testops.validation.groups.Created;
import io.vanguard.testops.validation.groups.Updated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomFieldUpdateRequestDefinition {
    @NotBlank(groups = {Updated.class})
    @Size(min = 1, max = 50, groups = {Updated.class})
    private String id;

    @NotBlank(groups = {Created.class})
    @Size(min = 1, max = 255, groups = {Created.class, Updated.class})
    private String name;

    @NotBlank(groups = {Created.class})
    @Size(min = 1, max = 30, groups = {Created.class})
    private String scene;

    @NotBlank(groups = {Created.class})
    @EnumValue(enumClass = CustomFieldType.class, groups = {Created.class, Updated.class})
    @Size(min = 1, max = 30, groups = {Created.class, Updated.class})
    private String type;

    @NotBlank(groups = {Created.class})
    @Size(min = 1, max = 50, groups = {Created.class})
    private String scopeId;
}
