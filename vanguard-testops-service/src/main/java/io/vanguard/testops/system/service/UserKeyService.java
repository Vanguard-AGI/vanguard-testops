package io.vanguard.testops.system.service;


import io.vanguard.testops.sdk.constants.HttpMethodConstants;
import io.vanguard.testops.sdk.constants.OperationLogConstants;
import io.vanguard.testops.sdk.exception.MSException;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.sdk.util.Translator;
import io.vanguard.testops.system.domain.UserKey;
import io.vanguard.testops.system.domain.UserKeyExample;
import io.vanguard.testops.system.dto.UserKeyDTO;
import io.vanguard.testops.system.dto.builder.LogDTOBuilder;
import io.vanguard.testops.system.log.constants.OperationLogModule;
import io.vanguard.testops.system.log.constants.OperationLogType;
import io.vanguard.testops.system.log.dto.LogDTO;
import io.vanguard.testops.system.log.service.OperationLogService;
import io.vanguard.testops.system.mapper.UserKeyMapper;
import io.vanguard.testops.system.uid.IDGenerator;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import io.vanguard.testops.sdk.util.LogUtils;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class UserKeyService {

    @Resource
    private UserKeyMapper userKeyMapper;

    @Resource
    private UserLoginService userLoginService;
    @Resource
    private OperationLogService operationLogService;

    public List<UserKey> getUserKeysInfo(String userId) {
        UserKeyExample userKeysExample = new UserKeyExample();
        userKeysExample.createCriteria().andCreateUserEqualTo(userId);
        userKeysExample.setOrderByClause("create_time");
        return userKeyMapper.selectByExample(userKeysExample);
    }

    public void add(String userId) {
        if (userLoginService.getUserDTO(userId) == null) {
            throw new MSException(Translator.get("user_not_exist") + userId);
        }
        UserKeyExample userKeysExample = new UserKeyExample();
        userKeysExample.createCriteria().andCreateUserEqualTo(userId);
        List<UserKey> userKeysList = userKeyMapper.selectByExample(userKeysExample);

        if (!CollectionUtils.isEmpty(userKeysList) && userKeysList.size() >= 5) {
            throw new MSException(Translator.get("user_apikey_limit"));
        }

        UserKey userKeys = new UserKey();
        userKeys.setId(IDGenerator.nextStr());
        userKeys.setCreateUser(userId);
        userKeys.setEnable(true);
        userKeys.setAccessKey(RandomStringUtils.randomAlphanumeric(16));
        userKeys.setSecretKey(RandomStringUtils.randomAlphanumeric(16));
        userKeys.setCreateTime(System.currentTimeMillis());
        userKeys.setForever(true);
        userKeyMapper.insert(userKeys);

        LogDTO dto = LogDTOBuilder.builder()
                .projectId(OperationLogConstants.SYSTEM)
                .organizationId(OperationLogConstants.SYSTEM)
                .type(OperationLogType.ADD.name())
                .module(OperationLogModule.PERSONAL_INFORMATION_APIKEYS)
                .method(HttpMethodConstants.GET.name())
                .path("/user/api/key/add")
                .sourceId(userKeys.getId())
                .content(userKeys.getAccessKey())
                .originalValue(JSON.toJSONBytes(userKeys))
                .build().getLogDTO();
        operationLogService.add(dto);
    }

    public void deleteUserKey(String id) {
        checkUserKey(id);
        userKeyMapper.deleteByPrimaryKey(id);
    }

    public void enableUserKey(String id) {
        checkUserKey(id);
        UserKey userKeys = new UserKey();
        userKeys.setId(id);
        userKeys.setEnable(true);
        userKeyMapper.updateByPrimaryKeySelective(userKeys);
    }

    public void disableUserKey(String id) {
        checkUserKey(id);
        UserKey userKeys = new UserKey();
        userKeys.setId(id);
        userKeys.setEnable(false);
        userKeyMapper.updateByPrimaryKeySelective(userKeys);
    }

    public UserKey getUserKey(String accessKey) {
        UserKeyExample userKeyExample = new UserKeyExample();
        userKeyExample.createCriteria().andAccessKeyEqualTo(accessKey).andEnableEqualTo(true);
        List<UserKey> userKeysList = userKeyMapper.selectByExample(userKeyExample);
        if (!CollectionUtils.isEmpty(userKeysList)) {
            return userKeysList.getFirst();
        }
        return null;
    }

    public void updateUserKey(UserKeyDTO userKeyDTO) {
        UserKey userKey = checkUserKey(userKeyDTO.getId());
        userKey.setId(userKeyDTO.getId());
        userKey.setForever(userKeyDTO.getForever());
        if (BooleanUtils.isFalse(userKeyDTO.getForever())) {
            if (userKeyDTO.getExpireTime() == null) {
                throw new MSException(Translator.get("expire_time_not_null"));
            }
            userKey.setExpireTime(userKeyDTO.getExpireTime());
        } else {
            userKey.setExpireTime(null);
        }
        userKey.setDescription(userKeyDTO.getDescription());
        userKeyMapper.updateByPrimaryKeySelective(userKey);
    }

    public UserKey checkUserKey(String id) {
        UserKey userKey = userKeyMapper.selectByPrimaryKey(id);
        if (userKey == null) {
            throw new MSException(Translator.get("api_key_not_exist"));
        }
        return userKey;
    }

    public void checkUserKeyOwner(String id, String userId) {
        UserKey userKey = checkUserKey(id);
        if (!StringUtils.equals(userKey.getCreateUser(), userId)) {
            throw new MSException(Translator.get("current_user_can_not_operation_api_key"));
        }
    }

    /**
     * 确保用户有可用的API密钥，如果没有则自动创建一个
     * @param userId 用户ID
     * @return 用户的API密钥列表
     */
    public List<UserKey> ensureUserHasApiKey(String userId) {
        List<UserKey> userKeys = getUserKeysInfo(userId);
        if (CollectionUtils.isEmpty(userKeys)) {
            LogUtils.info("用户 {} 没有API密钥，自动创建一个默认密钥", userId);
            add(userId);
            userKeys = getUserKeysInfo(userId);
        }
        return userKeys;
    }

    /**
     * 获取用户启用的API密钥列表
     * @param userId 用户ID
     * @return 启用的API密钥列表
     */
    public List<UserKey> getEnabledUserKeys(String userId) {
        UserKeyExample userKeysExample = new UserKeyExample();
        userKeysExample.createCriteria().andCreateUserEqualTo(userId).andEnableEqualTo(true);
        userKeysExample.setOrderByClause("create_time desc");
        return userKeyMapper.selectByExample(userKeysExample);
    }

    /**
     * 验证API密钥是否有效
     * @param accessKey 访问密钥
     * @return 如果有效返回UserKey对象，否则返回null
     */
    public UserKey validateApiKey(String accessKey) {
        if (StringUtils.isBlank(accessKey)) {
            return null;
        }
        
        UserKey userKey = getUserKey(accessKey);
        if (userKey == null) {
            return null;
        }
        
        // 检查是否启用
        if (BooleanUtils.isFalse(userKey.getEnable())) {
            LogUtils.warn("API密钥 {} 已被禁用", accessKey);
            return null;
        }
        
        // 检查是否过期
        if (BooleanUtils.isFalse(userKey.getForever())) {
            if (userKey.getExpireTime() != null && userKey.getExpireTime() < System.currentTimeMillis()) {
                LogUtils.warn("API密钥 {} 已过期", accessKey);
                return null;
            }
        }
        
        return userKey;
    }

    /**
     * 批量禁用用户的API密钥
     * @param userId 用户ID
     * @param operator 操作者
     */
    public void disableAllUserKeys(String userId, String operator) {
        List<UserKey> userKeys = getUserKeysInfo(userId);
        for (UserKey userKey : userKeys) {
            if (BooleanUtils.isTrue(userKey.getEnable())) {
                UserKey updateKey = new UserKey();
                updateKey.setId(userKey.getId());
                updateKey.setEnable(false);
                userKeyMapper.updateByPrimaryKeySelective(updateKey);
                
                LogUtils.info("禁用用户 {} 的API密钥: {}", userId, userKey.getAccessKey());
            }
        }
    }

    /**
     * 重新生成API密钥
     * @param id API密钥ID
     * @param userId 用户ID
     * @return 新的API密钥信息
     */
    public UserKey regenerateApiKey(String id, String userId) {
        checkUserKeyOwner(id, userId);
        
        UserKey userKey = checkUserKey(id);
        
        // 生成新的密钥
        String newAccessKey = RandomStringUtils.randomAlphanumeric(16);
        String newSecretKey = RandomStringUtils.randomAlphanumeric(16);
        
        UserKey updateKey = new UserKey();
        updateKey.setId(id);
        updateKey.setAccessKey(newAccessKey);
        updateKey.setSecretKey(newSecretKey);
        
        userKeyMapper.updateByPrimaryKeySelective(updateKey);
        
        LogUtils.info("用户 {} 重新生成API密钥: {}", userId, newAccessKey);
        
        // 返回更新后的密钥信息
        return userKeyMapper.selectByPrimaryKey(id);
    }
}
