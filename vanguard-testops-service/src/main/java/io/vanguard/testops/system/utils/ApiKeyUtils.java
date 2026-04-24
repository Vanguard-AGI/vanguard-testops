package io.vanguard.testops.system.utils;

import io.vanguard.testops.sdk.util.CodingUtils;
import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.sdk.util.CommonBeanFactory;
import io.vanguard.testops.system.domain.UserKey;
import io.vanguard.testops.system.service.UserKeyService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * API密钥工具类
 * 提供API密钥的生成、验证、签名等功能
 */
public class ApiKeyUtils {

    /**
     * 生成API密钥对
     * @return 包含accessKey和secretKey的Map
     */
    public static Map<String, String> generateApiKeyPair() {
        Map<String, String> keyPair = new HashMap<>();
        keyPair.put("accessKey", RandomStringUtils.randomAlphanumeric(16));
        keyPair.put("secretKey", RandomStringUtils.randomAlphanumeric(16));
        return keyPair;
    }

    /**
     * 生成API密钥签名
     * @param accessKey 访问密钥
     * @param secretKey 密钥
     * @param timestamp 时间戳
     * @return 签名
     */
    public static String generateSignature(String accessKey, String secretKey, long timestamp) {
        if (StringUtils.isBlank(accessKey) || StringUtils.isBlank(secretKey)) {
            throw new IllegalArgumentException("accessKey和secretKey不能为空");
        }
        
        String content = accessKey + "|" + timestamp;
        return CodingUtils.aesEncrypt(content, secretKey, accessKey);
    }

    /**
     * 生成当前时间的API密钥签名
     * @param accessKey 访问密钥
     * @param secretKey 密钥
     * @return 签名
     */
    public static String generateSignature(String accessKey, String secretKey) {
        return generateSignature(accessKey, secretKey, System.currentTimeMillis());
    }

    /**
     * 验证API密钥签名
     * @param accessKey 访问密钥
     * @param secretKey 密钥
     * @param signature 签名
     * @param maxAge 最大有效期（毫秒）
     * @return 是否有效
     */
    public static boolean validateSignature(String accessKey, String secretKey, String signature, long maxAge) {
        try {
            if (StringUtils.isBlank(accessKey) || StringUtils.isBlank(secretKey) || StringUtils.isBlank(signature)) {
                return false;
            }
            
            String decrypted = CodingUtils.aesDecrypt(signature, secretKey, accessKey);
            String[] parts = StringUtils.split(decrypted, "|");
            
            if (parts.length < 2) {
                return false;
            }
            
            if (!StringUtils.equals(accessKey, parts[0])) {
                return false;
            }
            
            long timestamp = Long.parseLong(parts[1]);
            long currentTime = System.currentTimeMillis();
            
            return Math.abs(currentTime - timestamp) <= maxAge;
        } catch (Exception e) {
            LogUtils.error("验证API密钥签名失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证API密钥签名（默认30分钟有效期）
     * @param accessKey 访问密钥
     * @param secretKey 密钥
     * @param signature 签名
     * @return 是否有效
     */
    public static boolean validateSignature(String accessKey, String secretKey, String signature) {
        return validateSignature(accessKey, secretKey, signature, 30 * 60 * 1000); // 30分钟
    }

    /**
     * 为指定用户创建默认API密钥
     * @param userId 用户ID
     * @param userName 用户名
     * @return 创建的API密钥信息
     */
    public static UserKey createDefaultApiKey(String userId, String userName) {
        try {
            LogUtils.info("为用户 {} ({}) 创建默认API密钥", userId, userName);
            
            UserKeyService userKeyService = CommonBeanFactory.getBean(UserKeyService.class);
            userKeyService.add(userId);
            
            // 获取刚创建的API密钥
            var userKeys = userKeyService.getUserKeysInfo(userId);
            if (!userKeys.isEmpty()) {
                UserKey apiKey = userKeys.get(userKeys.size() - 1); // 获取最新创建的
                LogUtils.info("用户 {} ({}) 的默认API密钥创建成功: {}", userId, userName, apiKey.getAccessKey());
                return apiKey;
            }
            
            return null;
        } catch (Exception e) {
            LogUtils.error("为用户创建默认API密钥失败: {} - {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * 确保用户有可用的API密钥
     * @param userId 用户ID
     * @return 用户的API密钥列表
     */
    public static java.util.List<UserKey> ensureUserHasApiKey(String userId) {
        try {
            UserKeyService userKeyService = CommonBeanFactory.getBean(UserKeyService.class);
            return userKeyService.ensureUserHasApiKey(userId);
        } catch (Exception e) {
            LogUtils.error("确保用户有API密钥失败: {} - {}", userId, e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    /**
     * 验证API密钥是否有效
     * @param accessKey 访问密钥
     * @return 如果有效返回UserKey对象，否则返回null
     */
    public static UserKey validateApiKey(String accessKey) {
        try {
            if (StringUtils.isBlank(accessKey)) {
                return null;
            }
            
            UserKeyService userKeyService = CommonBeanFactory.getBean(UserKeyService.class);
            return userKeyService.validateApiKey(accessKey);
        } catch (Exception e) {
            LogUtils.error("验证API密钥异常: {} - {}", accessKey, e.getMessage());
            return null;
        }
    }

    /**
     * 获取API密钥的完整信息（包括用户信息）
     * @param accessKey 访问密钥
     * @return API密钥详细信息
     */
    public static ApiKeyInfo getApiKeyInfo(String accessKey) {
        try {
            UserKey userKey = validateApiKey(accessKey);
            if (userKey == null) {
                return null;
            }
            
            ApiKeyInfo info = new ApiKeyInfo();
            info.setAccessKey(userKey.getAccessKey());
            info.setCreateTime(userKey.getCreateTime());
            info.setEnable(userKey.getEnable());
            info.setForever(userKey.getForever());
            info.setExpireTime(userKey.getExpireTime());
            info.setDescription(userKey.getDescription());
            info.setUserId(userKey.getCreateUser());
            
            return info;
        } catch (Exception e) {
            LogUtils.error("获取API密钥信息异常: {} - {}", accessKey, e.getMessage());
            return null;
        }
    }

    /**
     * API密钥详细信息
     */
    public static class ApiKeyInfo {
        private String accessKey;
        private Long createTime;
        private Boolean enable;
        private Boolean forever;
        private Long expireTime;
        private String description;
        private String userId;

        // Getters and Setters
        public String getAccessKey() { return accessKey; }
        public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
        
        public Long getCreateTime() { return createTime; }
        public void setCreateTime(Long createTime) { this.createTime = createTime; }
        
        public Boolean getEnable() { return enable; }
        public void setEnable(Boolean enable) { this.enable = enable; }
        
        public Boolean getForever() { return forever; }
        public void setForever(Boolean forever) { this.forever = forever; }
        
        public Long getExpireTime() { return expireTime; }
        public void setExpireTime(Long expireTime) { this.expireTime = expireTime; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }
}
