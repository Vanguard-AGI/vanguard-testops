package io.vanguard.testops.system.service;

import io.vanguard.testops.system.domain.UserInvite;
import io.vanguard.testops.system.dto.request.UserRegisterRequest;
import io.vanguard.testops.system.dto.user.request.UserBatchCreateRequest;

import java.util.List;

/**
 * 系统用户相关接口
 */
public interface UserXpackService {

    int GWHowToAddUser(UserBatchCreateRequest userCreateDTO, String source, String operator);

    int GWHowToAddUser(UserRegisterRequest registerRequest, UserInvite userInvite) throws Exception;

    int GWHowToChangeUser(List<String> userIds, boolean enable, String operator);

    int GWHowToDeleteUser(List<String> userIdList, String operator);
}
