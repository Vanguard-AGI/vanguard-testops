package io.vanguard.testops.system.mapper;

import io.vanguard.testops.system.domain.UserRoleRelation;
import io.vanguard.testops.system.dto.sdk.ExcludeOptionDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BaseUserRoleRelationMapper {
    List<UserRoleRelation> getUserIdAndSourceIdByUserIds(@Param("userIds") List<String> userIds);

    List<String> getUserIdByRoleId(@Param("roleId") String roleId);

    List<ExcludeOptionDTO> getSelectOption(@Param("roleId") String roleId);
}
