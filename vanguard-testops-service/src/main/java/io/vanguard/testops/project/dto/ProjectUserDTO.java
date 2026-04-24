package io.vanguard.testops.project.dto;

import io.vanguard.testops.system.domain.User;
import io.vanguard.testops.system.domain.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author Jan
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ProjectUserDTO extends User {

    @Schema(description =  "用户组集合")
    private List<UserRole> userRoles;
}
