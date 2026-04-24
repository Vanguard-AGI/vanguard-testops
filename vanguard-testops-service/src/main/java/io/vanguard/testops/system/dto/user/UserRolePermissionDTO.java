package io.vanguard.testops.system.dto.user;

import io.vanguard.testops.system.domain.UserRole;
import io.vanguard.testops.system.domain.UserRoleRelation;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserRolePermissionDTO {
    List<UserRoleResourceDTO> list = new ArrayList<>();
    List<UserRole> userRoles = new ArrayList<>();
    List<UserRoleRelation> userRoleRelations = new ArrayList<>();
}
