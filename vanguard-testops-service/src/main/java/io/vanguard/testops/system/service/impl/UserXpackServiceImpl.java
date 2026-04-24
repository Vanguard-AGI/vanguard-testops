package io.vanguard.testops.system.service.impl;

import io.vanguard.testops.sdk.constants.UserSource;
import io.vanguard.testops.sdk.util.CommonBeanFactory;
import io.vanguard.testops.sdk.util.CodingUtils;
import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.system.domain.User;
import io.vanguard.testops.system.domain.UserInvite;
import io.vanguard.testops.system.domain.UserRoleRelation;
import io.vanguard.testops.system.dto.request.UserRegisterRequest;
import io.vanguard.testops.system.dto.user.UserCreateInfo;
import io.vanguard.testops.system.dto.user.request.UserBatchCreateRequest;
import io.vanguard.testops.system.uid.IDGenerator;
import io.vanguard.testops.system.mapper.UserMapper;
import io.vanguard.testops.system.mapper.UserRoleRelationMapper;
import io.vanguard.testops.system.service.UserXpackService;
import io.vanguard.testops.system.utils.SessionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import io.vanguard.testops.sdk.util.EncryptUtils;
import io.vanguard.testops.system.service.UserKeyService;

@Service
public class UserXpackServiceImpl implements UserXpackService {

    // 默认组织ID
    private static final String DEFAULT_ORGANIZATION_ID = "100001";
    // 默认项目ID
    private static final String DEFAULT_PROJECT_ID = "100001100001";
    // 系统成员用户组ID
    private static final String SYSTEM_MEMBER_ROLE_ID = "member";

    @Override
    public int GWHowToAddUser(UserBatchCreateRequest userCreateDTO, String source, String operator) {
        try {
            LogUtils.info("开始批量创建用户，用户数量: {}, 来源: {}, 操作者: {}", 
                userCreateDTO.getUserInfoList().size(), source, operator);
            
            UserMapper userMapper = CommonBeanFactory.getBean(UserMapper.class);
            UserRoleRelationMapper userRoleRelationMapper = CommonBeanFactory.getBean(UserRoleRelationMapper.class);
            
            for (UserCreateInfo userInfo : userCreateDTO.getUserInfoList()) {
                User user = new User();
                user.setId(IDGenerator.nextStr());
                user.setName(userInfo.getName());
                user.setEmail(userInfo.getEmail());
                user.setPhone(userInfo.getPhone());
                // 设置默认密码为邮箱的MD5值
                user.setPassword(CodingUtils.md5(userInfo.getEmail()));
                user.setEnable(true);
                
                // 设置时间戳，确保creationTime不为null
                long currentTime = System.currentTimeMillis();
                user.setCreateTime(currentTime);
                user.setUpdateTime(currentTime);
                user.setCreateUser(operator);
                user.setUpdateUser(operator);
                user.setDeleted(false);
                
                // 统一设置为LOCAL来源，避免用户创建途径异常
                user.setSource(UserSource.LOCAL.name());
                // 设置默认语言为zh-CN，避免必填字段缺失
                user.setLanguage("zh-CN");
                // 设置默认组织ID，避免权限检查失败 - 使用正确的默认组织ID 100001
                user.setLastOrganizationId(DEFAULT_ORGANIZATION_ID);
                // 设置默认项目ID，让新用户自动分配到示例项目
                user.setLastProjectId(DEFAULT_PROJECT_ID);
                
                // 生成cft_token - 使用SHA-256哈希 + Base64编码
                String cftToken;
                try {
                    cftToken = generateCftToken(user.getId(), currentTime);
                    user.setCftToken(cftToken);
                } catch (Exception e) {
                    LogUtils.error("为用户 {} 生成cft_token失败: {}", user.getId(), e.getMessage());
                    throw new RuntimeException("用户创建失败：无法生成cft_token", e);
                }
                
                LogUtils.info("创建用户: {}, email: {}, source: {}, cft_token: {}, last_organization_id: {}", 
                    user.getName(), user.getEmail(), user.getSource(), cftToken, user.getLastOrganizationId());
                
                // 插入用户
                if (userMapper != null) {
                    userMapper.insert(user);
                }

                // 为新用户分配指定的用户组
                assignUserRoles(user.getId(), userCreateDTO.getUserRoleIdList(), operator, userRoleRelationMapper);
                
                // 为新用户自动创建默认API密钥
                createDefaultApiKey(user.getId(), user.getName());
            }
            
            LogUtils.info("用户创建成功，共创建 {} 个用户", userCreateDTO.getUserInfoList().size());
            return 0; // 返回0表示成功
        } catch (Exception e) {
            LogUtils.error("用户创建失败: {}", e.getMessage());
            return -1; // 返回-1表示失败
        }
    }

    @Override
    public int GWHowToAddUser(UserRegisterRequest registerRequest, UserInvite userInvite) throws Exception {
        try {
            LogUtils.info("开始通过邀请创建用户，邮箱: {}", userInvite.getEmail());
            
            User user = new User();
            user.setId(IDGenerator.nextStr());
            user.setName(registerRequest.getName());
            user.setEmail(userInvite.getEmail());
            user.setPassword(registerRequest.getPassword());
            user.setPhone(registerRequest.getPhone());
            user.setEnable(true);
            
            // 设置时间戳，确保creationTime不为null
            long currentTime = System.currentTimeMillis();
            user.setCreateTime(currentTime);
            user.setUpdateTime(currentTime);
            user.setCreateUser(userInvite.getInviteUser());
            user.setUpdateUser(userInvite.getInviteUser());
            user.setDeleted(false);
            
            // 统一设置为LOCAL来源，避免用户创建途径异常
            user.setSource(UserSource.LOCAL.name());
            // 设置默认语言为zh-CN，避免必填字段缺失
            user.setLanguage("zh-CN");
            // 设置默认组织ID，避免权限检查失败 - 使用正确的默认组织ID 100001
            user.setLastOrganizationId(DEFAULT_ORGANIZATION_ID);
            
            // 生成cft_token - 使用SHA-256哈希 + Base64编码
            String cftToken;
            try {
                cftToken = generateCftToken(user.getId(), currentTime);
                user.setCftToken(cftToken);
            } catch (Exception e) {
                LogUtils.error("为用户 {} 生成cft_token失败: {}", user.getId(), e.getMessage());
                throw new RuntimeException("通过邀请创建用户失败：无法生成cft_token", e);
            }
            
            LogUtils.info("通过邀请创建用户: {}, email: {}, source: {}, cft_token: {}, last_organization_id: {}", 
                user.getName(), user.getEmail(), user.getSource(), cftToken, user.getLastOrganizationId());
            
            // 插入用户
            UserMapper userMapper = CommonBeanFactory.getBean(UserMapper.class);
            if (userMapper != null) {
                userMapper.insert(user);
            }

            // 为新用户分配默认组织和系统成员用户组
            UserRoleRelationMapper userRoleRelationMapper = CommonBeanFactory.getBean(UserRoleRelationMapper.class);
            assignDefaultOrganizationAndRole(user.getId(), userInvite.getInviteUser(), userRoleRelationMapper);
            
            // 为新用户自动创建默认API密钥
            createDefaultApiKey(user.getId(), user.getName());
            
            LogUtils.info("通过邀请创建用户成功");
            return 0; // 返回0表示成功
        } catch (Exception e) {
            LogUtils.error("通过邀请创建用户失败: {}", e.getMessage());
            return -1; // 返回-1表示失败
        }
    }

    /**
     * 为用户分配指定的用户组
     */
    private void assignUserRoles(String userId, List<String> roleIds, String operator, UserRoleRelationMapper userRoleRelationMapper) {
        try {
            LogUtils.info("为用户 {} 分配用户组: {}", userId, roleIds);

            if (roleIds != null && !roleIds.isEmpty()) {
                for (String roleId : roleIds) {
                    UserRoleRelation userRoleRelation = new UserRoleRelation();
                    userRoleRelation.setId(IDGenerator.nextStr());
                    userRoleRelation.setUserId(userId);
                    userRoleRelation.setRoleId(roleId);
                    // 根据角色类型设置不同的 sourceId 和 organizationId
                    if (SYSTEM_MEMBER_ROLE_ID.equals(roleId)) {
                        // 系统角色使用 system
                        userRoleRelation.setSourceId("system");
                        userRoleRelation.setOrganizationId("system");
                    } else {
                        // 其他角色使用默认组织
                        userRoleRelation.setSourceId(DEFAULT_ORGANIZATION_ID);
                        userRoleRelation.setOrganizationId(DEFAULT_ORGANIZATION_ID);
                    }
                    userRoleRelation.setCreateTime(System.currentTimeMillis());
                    userRoleRelation.setCreateUser(operator);

                    userRoleRelationMapper.insert(userRoleRelation);
                }
            }

            // 分配默认的member角色，确保用户有基本权限
            UserRoleRelation memberRelation = new UserRoleRelation();
            memberRelation.setId(IDGenerator.nextStr());
            memberRelation.setUserId(userId);
            memberRelation.setRoleId(SYSTEM_MEMBER_ROLE_ID); // 系统成员用户组，默认权限
            memberRelation.setSourceId("system");
            memberRelation.setOrganizationId("system");
            memberRelation.setCreateTime(System.currentTimeMillis());
            memberRelation.setCreateUser(operator);
            userRoleRelationMapper.insert(memberRelation);

            // 分配组织成员角色，确保用户属于默认组织
            UserRoleRelation orgMemberRelation = new UserRoleRelation();
            orgMemberRelation.setId(IDGenerator.nextStr());
            orgMemberRelation.setUserId(userId);
            orgMemberRelation.setRoleId("org_member"); // 组织成员角色
            orgMemberRelation.setSourceId(DEFAULT_ORGANIZATION_ID);
            orgMemberRelation.setOrganizationId(DEFAULT_ORGANIZATION_ID);
            orgMemberRelation.setCreateTime(System.currentTimeMillis());
            orgMemberRelation.setCreateUser(operator);
            userRoleRelationMapper.insert(orgMemberRelation);

            // 分配项目成员角色，确保用户属于示例项目
            UserRoleRelation projectMemberRelation = new UserRoleRelation();
            projectMemberRelation.setId(IDGenerator.nextStr());
            projectMemberRelation.setUserId(userId);
            projectMemberRelation.setRoleId("project_member"); // 项目成员角色
            projectMemberRelation.setSourceId(DEFAULT_PROJECT_ID);
            projectMemberRelation.setOrganizationId(DEFAULT_ORGANIZATION_ID);
            projectMemberRelation.setCreateTime(System.currentTimeMillis());
            projectMemberRelation.setCreateUser(operator);
            userRoleRelationMapper.insert(projectMemberRelation);

            LogUtils.info("用户 {} 的用户组分配成功", userId);
        } catch (Exception e) {
            LogUtils.error("为用户 {} 分配用户组失败: {}", userId, e.getMessage());
            throw e;
        }
    }

    /**
     * 为用户分配默认组织和用户组
     */
    private void assignDefaultOrganizationAndRole(String userId, String operator, UserRoleRelationMapper userRoleRelationMapper) {
        try {
            LogUtils.info("为用户 {} 分配默认组织和用户组", userId);

            // 分配默认的member角色
            UserRoleRelation memberRelation = new UserRoleRelation();
            memberRelation.setId(IDGenerator.nextStr());
            memberRelation.setUserId(userId);
            memberRelation.setRoleId(SYSTEM_MEMBER_ROLE_ID); // 系统成员用户组，默认权限
            memberRelation.setSourceId("system");
            memberRelation.setOrganizationId("system");
            memberRelation.setCreateTime(System.currentTimeMillis());
            memberRelation.setCreateUser(operator);
            userRoleRelationMapper.insert(memberRelation);

            // 分配组织成员角色，确保用户属于默认组织
            UserRoleRelation orgMemberRelation = new UserRoleRelation();
            orgMemberRelation.setId(IDGenerator.nextStr());
            orgMemberRelation.setUserId(userId);
            orgMemberRelation.setRoleId("org_member"); // 组织成员角色
            orgMemberRelation.setSourceId(DEFAULT_ORGANIZATION_ID);
            orgMemberRelation.setOrganizationId(DEFAULT_ORGANIZATION_ID);
            orgMemberRelation.setCreateTime(System.currentTimeMillis());
            orgMemberRelation.setCreateUser(operator);
            userRoleRelationMapper.insert(orgMemberRelation);

            // 分配项目成员角色，确保用户属于示例项目
            UserRoleRelation projectMemberRelation = new UserRoleRelation();
            projectMemberRelation.setId(IDGenerator.nextStr());
            projectMemberRelation.setUserId(userId);
            projectMemberRelation.setRoleId("project_member"); // 项目成员角色
            projectMemberRelation.setSourceId(DEFAULT_PROJECT_ID);
            projectMemberRelation.setOrganizationId(DEFAULT_ORGANIZATION_ID);
            projectMemberRelation.setCreateTime(System.currentTimeMillis());
            projectMemberRelation.setCreateUser(operator);
            userRoleRelationMapper.insert(projectMemberRelation);

            LogUtils.info("用户 {} 的默认组织和用户组分配成功，分配member角色、组织成员角色和项目成员角色", userId);
        } catch (Exception e) {
            LogUtils.error("为用户 {} 分配默认组织和用户组失败: {}", userId, e.getMessage());
            throw e;
        }
    }

    @Override
    public int GWHowToChangeUser(List<String> userIds, boolean enable, String operator) {
        try {
            LogUtils.info("开始{}用户，用户数量: {}, 操作者: {}", enable ? "启用" : "禁用", userIds.size(), operator);
            
            UserMapper userMapper = CommonBeanFactory.getBean(UserMapper.class);
            for (String userId : userIds) {
                User user = new User();
                user.setId(userId);
                user.setEnable(enable);
                user.setUpdateTime(System.currentTimeMillis());
                user.setUpdateUser(operator);
                userMapper.updateByPrimaryKeySelective(user);
            }
            
            LogUtils.info("用户{}成功", enable ? "启用" : "禁用");
            return 0; // 返回0表示成功
        } catch (Exception e) {
            LogUtils.error("用户操作失败: {} - {}", enable ? "启用" : "禁用", e.getMessage());
            return -1; // 返回-1表示失败
        }
    }

    @Override
    public int GWHowToDeleteUser(List<String> userIds, String operator) {
        try {
            LogUtils.info("开始删除用户，用户数量: {}, 操作者: {}", userIds.size(), operator);
            
            UserMapper userMapper = CommonBeanFactory.getBean(UserMapper.class);
            for (String userId : userIds) {
                User user = new User();
                user.setId(userId);
                user.setDeleted(true);
                user.setUpdateTime(System.currentTimeMillis());
                user.setUpdateUser(operator);
                userMapper.updateByPrimaryKeySelective(user);
            }
            
            LogUtils.info("用户删除成功");
            return 0; // 返回0表示成功
        } catch (Exception e) {
            LogUtils.error("用户删除失败: {}", e.getMessage());
            return -1; // 返回-1表示失败
        }
    }

    /**
     * 生成cft_token
     * 使用正确的AES加密逻辑：userId → AES加密 → Base64编码
     */
    private String generateCftToken(String userId, long timestamp) throws Exception {
        // 特殊处理admin用户，使用硬编码的cftToken
        if ("admin".equals(userId)) {
            return "/sU3a0dtnm5Ykmyj8LfCjA==";
        }
        
        // 使用正确的密钥和IV进行AES加密
        String key = "@mscloudiofit.zy";
        String iv = "4561234567890123";
        
        // 确保密钥和IV的长度符合AES要求
        byte[] keyBytes = ensureKeyLength(key, 16); // AES-128
        byte[] ivBytes = ensureKeyLength(iv, 16); // 16字节IV
        
        javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
        javax.crypto.spec.IvParameterSpec ivSpec = new javax.crypto.spec.IvParameterSpec(ivBytes);
        
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        
        byte[] encrypted = cipher.doFinal(userId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String cftToken = java.util.Base64.getEncoder().encodeToString(encrypted).trim();
        
        LogUtils.info("为用户 {} 生成cft_token: {} (用户ID: {})", userId, cftToken, userId);
        return cftToken;
    }
    
    /**
     * 确保密钥长度符合AES要求
     */
    private byte[] ensureKeyLength(String key, int length) {
        byte[] keyBytes = key.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (keyBytes.length == length) {
            return keyBytes;
        }
        
        // 如果密钥长度不符合要求，进行调整
        byte[] adjustedKey = new byte[length];
        System.arraycopy(keyBytes, 0, adjustedKey, 0, Math.min(keyBytes.length, length));
        return adjustedKey;
    }

    /**
     * 为新用户自动创建默认API密钥
     * @param userId 用户ID
     * @param userName 用户名
     */
    private void createDefaultApiKey(String userId, String userName) {
        try {
            LogUtils.info("为用户 {} ({}) 创建默认API密钥", userId, userName);
            
            UserKeyService userKeyService = CommonBeanFactory.getBean(UserKeyService.class);
            userKeyService.add(userId);
            
            LogUtils.info("用户 {} ({}) 的默认API密钥创建成功", userId, userName);
        } catch (Exception e) {
            LogUtils.error("创建默认API密钥失败: {} - {}", userId, e.getMessage());
            // 注意：这里不抛出异常，避免影响用户创建流程
            // API密钥创建失败不应该阻止用户创建
        }
    }
}
