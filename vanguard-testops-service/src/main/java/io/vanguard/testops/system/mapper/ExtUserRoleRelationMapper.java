package io.vanguard.testops.system.mapper;

import io.vanguard.testops.system.domain.UserRoleRelation;
import io.vanguard.testops.system.dto.request.GlobalUserRoleRelationQueryRequest;
import io.vanguard.testops.system.dto.user.UserRoleOptionDto;
import io.vanguard.testops.system.dto.user.UserRoleRelationUserDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtUserRoleRelationMapper {
    List<UserRoleRelation> selectGlobalRoleByUserIdList(@Param("userIds") List<String> userIdList);

    List<UserRoleRelation> selectRoleByUserIdList(@Param("userIds") List<String> userIdList);

    List<UserRoleRelation> selectGlobalRoleByUserId(String userId);

    List<UserRoleRelationUserDTO> listGlobal(@Param("request") GlobalUserRoleRelationQueryRequest request);

    List<UserRoleOptionDto> selectUserRoleByUserIds(@Param("userIds") List<String> userIds, @Param("orgId") String orgId);

    List<UserRoleOptionDto> selectProjectUserRoleByUserIds(@Param("userIds") List<String> userIds, @Param("projectId") String projectId);
}
