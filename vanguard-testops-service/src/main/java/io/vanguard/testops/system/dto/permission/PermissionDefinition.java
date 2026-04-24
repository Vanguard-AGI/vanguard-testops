package io.vanguard.testops.system.dto.permission;


import io.vanguard.testops.system.dto.user.UserRoleResource;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 加载的权限定义
 * @author Jan
 */
@Data
public class PermissionDefinition implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<UserRoleResource> resource;
    private List<Permission> permissions;
}
