package io.vanguard.testops.system.security.handler;

import io.vanguard.testops.sdk.util.CodingUtils;
import io.vanguard.testops.sdk.util.CommonBeanFactory;
import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.system.domain.UserKey;
import io.vanguard.testops.system.service.UserKeyService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

public class ApiKeyHandler {

    public static final String API_ACCESS_KEY = "accessKey";

    public static final String API_SIGNATURE = "signature";

    public static String getUser(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return getUser(request.getHeader(API_ACCESS_KEY), request.getHeader(API_SIGNATURE));
    }

    public static Boolean isApiKeyCall(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        return !StringUtils.isBlank(request.getHeader(API_ACCESS_KEY)) && !StringUtils.isBlank(request.getHeader(API_SIGNATURE));
    }

    public static String getUser(String accessKey, String signature) {
        if (StringUtils.isBlank(accessKey) || StringUtils.isBlank(signature)) {
            LogUtils.warn("API密钥验证失败：accessKey或signature为空");
            return null;
        }
        
        UserKeyService userKeyService = CommonBeanFactory.getBean(UserKeyService.class);
        UserKey userKey = userKeyService.getUserKey(accessKey);
        
        if (userKey == null) {
            LogUtils.warn("API密钥验证失败：无效的accessKey: {}", accessKey);
            throw new RuntimeException("API密钥不存在，请检查accessKey是否正确或联系管理员创建API密钥");
        }
        
        if (BooleanUtils.isFalse(userKey.getEnable())) {
            LogUtils.warn("API密钥验证失败：API密钥已被禁用: {}", accessKey);
            throw new RuntimeException("API密钥已被禁用，请联系管理员");
        }
        
        if (BooleanUtils.isFalse(userKey.getForever())) {
            if (userKey.getExpireTime() == null || userKey.getExpireTime() < System.currentTimeMillis()) {
                LogUtils.warn("API密钥验证失败：API密钥已过期: {}", accessKey);
                throw new RuntimeException("API密钥已过期，请重新生成或联系管理员");
            }
        }
        
        String signatureDecrypt;
        try {
            signatureDecrypt = CodingUtils.aesDecrypt(signature, userKey.getSecretKey(), accessKey);
        } catch (Throwable t) {
            LogUtils.warn("API密钥验证失败：签名解密失败: {}, 错误: {}", accessKey, t.getMessage());
            throw new RuntimeException("API密钥签名无效，请检查签名是否正确");
        }
        
        String[] signatureArray = StringUtils.split(StringUtils.trimToNull(signatureDecrypt), "|");
        if (signatureArray.length < 2) {
            LogUtils.info("API密钥验证：兼容模式，直接使用accessKey/signature: {}", accessKey);
            return userKey.getCreateUser(); // TODO 兼容直接输入 AK/SK 并未拼接情况
        }
        
        if (!StringUtils.equals(accessKey, signatureArray[0])) {
            LogUtils.warn("API密钥验证失败：签名中的accessKey不匹配: {}", accessKey);
            throw new RuntimeException("API密钥签名无效");
        }
        
        long signatureTime;
        try {
            signatureTime = Long.parseLong(signatureArray[signatureArray.length - 1]);
        } catch (Exception e) {
            LogUtils.warn("API密钥验证失败：签名时间解析失败: {}, 错误: {}", accessKey, e.getMessage());
            throw new RuntimeException("API密钥签名格式错误");
        }
        
        if (Math.abs(System.currentTimeMillis() - signatureTime) > 1800000) {
            //签名30分钟超时
            LogUtils.warn("API密钥验证失败：签名已超时: {}, 签名时间: {}", accessKey, signatureTime);
            throw new RuntimeException("API密钥签名已超时，请重新生成签名");
        }
        
        LogUtils.info("API密钥验证成功: {}, 用户: {}", accessKey, userKey.getCreateUser());
        return userKey.getCreateUser();
    }

    /**
     * 确保用户有可用的API密钥，如果没有则自动创建一个
     * @param userId 用户ID
     * @return 是否成功确保API密钥存在
     */
    public static boolean ensureUserHasApiKey(String userId) {
        try {
            UserKeyService userKeyService = CommonBeanFactory.getBean(UserKeyService.class);
            userKeyService.ensureUserHasApiKey(userId);
            LogUtils.info("确保用户 {} 有可用的API密钥", userId);
            return true;
        } catch (Exception e) {
            LogUtils.error("确保用户 {} 有API密钥失败", userId, e);
            return false;
        }
    }

    /**
     * 验证API密钥是否有效（不抛出异常）
     * @param accessKey 访问密钥
     * @return 如果有效返回用户ID，否则返回null
     */
    public static String validateApiKeySilently(String accessKey) {
        try {
            if (StringUtils.isBlank(accessKey)) {
                return null;
            }
            
            UserKeyService userKeyService = CommonBeanFactory.getBean(UserKeyService.class);
            UserKey userKey = userKeyService.validateApiKey(accessKey);
            
            return userKey != null ? userKey.getCreateUser() : null;
        } catch (Exception e) {
            LogUtils.error("验证API密钥时发生异常: {}", accessKey, e);
            return null;
        }
    }
}
