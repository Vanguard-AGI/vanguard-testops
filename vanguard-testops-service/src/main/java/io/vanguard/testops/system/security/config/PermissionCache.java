package io.vanguard.testops.system.security.config;

import io.vanguard.testops.system.dto.permission.PermissionDefinitionItem;
import lombok.Data;

import java.util.List;

@Data
public class PermissionCache {
    private List<PermissionDefinitionItem> permissionDefinition;
}
