package io.vanguard.testops.system.service;

import io.vanguard.testops.system.dto.sdk.SessionUser;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * 飞书SSO服务接口
 */
public interface LarkSSOService {

    /**
     * 处理飞书登录回调
     *
     * @param request HTTP请求
     * @return 会话用户信息
     */
    SessionUser handleCallback(HttpServletRequest request);

    /**
     * 飞书登录
     *
     * @param code  授权码
     * @param state 状态参数
     * @return 会话用户信息
     */
    SessionUser login(String code, String state);

    /**
     * 获取飞书登录配置信息
     *
     * @return 配置信息
     */
    Map<String, Object> getLarkInfo();

    /**
     * 保存飞书配置
     *
     * @param config 配置信息
     * @return 保存结果
     */
    Map<String, Object> saveConfig(Map<String, Object> config);

    /**
     * 测试飞书连接
     *
     * @param config 配置信息
     * @return 测试结果
     */
    Map<String, Object> testConnection(Map<String, Object> config);

    /**
     * 清除前端配置
     */
    void clearFrontendConfig();
}
