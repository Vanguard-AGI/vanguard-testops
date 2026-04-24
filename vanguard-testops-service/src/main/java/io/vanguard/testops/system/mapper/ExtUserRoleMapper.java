package io.vanguard.testops.system.mapper;

import io.vanguard.testops.system.domain.User;
import io.vanguard.testops.system.dto.request.OrganizationUserRoleMemberRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtUserRoleMapper {
    List<String> selectGlobalRoleList(@Param("roleIdList") List<String> roleIdList, @Param("isSystem") boolean isSystem);

    List<User> listOrganizationRoleMember(@Param("request") OrganizationUserRoleMemberRequest request);
}
