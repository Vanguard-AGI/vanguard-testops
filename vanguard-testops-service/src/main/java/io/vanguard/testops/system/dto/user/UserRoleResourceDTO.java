package io.vanguard.testops.system.dto.user;

import io.vanguard.testops.system.domain.UserRole;
import io.vanguard.testops.system.domain.UserRolePermission;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class UserRoleResourceDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private UserRoleResource resource;
    private List<UserRolePermission> permissions;
    private String type;

    private UserRole userRole;
    private List<UserRolePermission> userRolePermissions;
}
