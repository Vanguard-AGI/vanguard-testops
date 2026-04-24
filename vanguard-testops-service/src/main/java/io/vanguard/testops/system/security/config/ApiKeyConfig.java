package io.vanguard.testops.system.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * API密钥配置类
 * 用于管理API密钥相关的配置参数
 */
@Component
@ConfigurationProperties(prefix = "metersphere.api-key")
public class ApiKeyConfig {

    /**
     * 每个用户最大API密钥数量
     */
    private int maxKeysPerUser = 5;

    /**
     * API密钥长度
     */
    private int keyLength = 16;

    /**
     * 签名有效期（毫秒）
     */
    private long signatureExpireTime = 30 * 60 * 1000; // 30分钟

    /**
     * 是否在用户创建时自动生成API密钥
     */
    private boolean autoCreateOnUserCreation = true;

    /**
     * 默认API密钥描述
     */
    private String defaultDescription = "系统自动生成的默认API密钥";

    /**
     * 是否启用API密钥功能
     */
    private boolean enabled = true;

    /**
     * 是否记录API密钥操作日志
     */
    private boolean enableLogging = true;

    /**
     * API密钥字符集
     */
    private String characterSet = "ALPHANUMERIC"; // ALPHANUMERIC, ALPHA, NUMERIC

    /**
     * 是否允许重复的API密钥（通常不建议）
     */
    private boolean allowDuplicateKeys = false;

    /**
     * API密钥过期时间（毫秒，0表示永不过期）
     */
    private long defaultExpireTime = 0; // 0表示永不过期

    // Getters and Setters
    public int getMaxKeysPerUser() {
        return maxKeysPerUser;
    }

    public void setMaxKeysPerUser(int maxKeysPerUser) {
        this.maxKeysPerUser = maxKeysPerUser;
    }

    public int getKeyLength() {
        return keyLength;
    }

    public void setKeyLength(int keyLength) {
        this.keyLength = keyLength;
    }

    public long getSignatureExpireTime() {
        return signatureExpireTime;
    }

    public void setSignatureExpireTime(long signatureExpireTime) {
        this.signatureExpireTime = signatureExpireTime;
    }

    public boolean isAutoCreateOnUserCreation() {
        return autoCreateOnUserCreation;
    }

    public void setAutoCreateOnUserCreation(boolean autoCreateOnUserCreation) {
        this.autoCreateOnUserCreation = autoCreateOnUserCreation;
    }

    public String getDefaultDescription() {
        return defaultDescription;
    }

    public void setDefaultDescription(String defaultDescription) {
        this.defaultDescription = defaultDescription;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnableLogging() {
        return enableLogging;
    }

    public void setEnableLogging(boolean enableLogging) {
        this.enableLogging = enableLogging;
    }

    public String getCharacterSet() {
        return characterSet;
    }

    public void setCharacterSet(String characterSet) {
        this.characterSet = characterSet;
    }

    public boolean isAllowDuplicateKeys() {
        return allowDuplicateKeys;
    }

    public void setAllowDuplicateKeys(boolean allowDuplicateKeys) {
        this.allowDuplicateKeys = allowDuplicateKeys;
    }

    public long getDefaultExpireTime() {
        return defaultExpireTime;
    }

    public void setDefaultExpireTime(long defaultExpireTime) {
        this.defaultExpireTime = defaultExpireTime;
    }
}
