package io.vanguard.testops.system.service.impl;

import io.vanguard.testops.sdk.constants.SessionConstants;
import io.vanguard.testops.sdk.constants.UserSource;
import io.vanguard.testops.sdk.exception.MSException;
import io.vanguard.testops.sdk.util.CodingUtils;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.system.controller.handler.result.MsHttpResultCode;
import io.vanguard.testops.system.domain.User;
import io.vanguard.testops.system.domain.UserExample;
import io.vanguard.testops.system.domain.UserRoleRelation;
import io.vanguard.testops.system.dto.sdk.SessionUser;
import io.vanguard.testops.system.dto.user.UserDTO;
import io.vanguard.testops.system.domain.SystemParameter;
import io.vanguard.testops.system.mapper.SystemParameterMapper;
import io.vanguard.testops.system.mapper.UserMapper;
import io.vanguard.testops.system.mapper.UserRoleRelationMapper;
import io.vanguard.testops.system.service.LarkSSOService;
import io.vanguard.testops.system.service.UserLoginService;
import io.vanguard.testops.system.service.UserXpackService;
import io.vanguard.testops.system.uid.IDGenerator;
import io.vanguard.testops.system.utils.SessionUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.session.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.DigestUtils;

/**
 * 飞书SSO服务实现类
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class LarkSSOServiceImpl implements LarkSSOService {

    // 默认组织ID
    private static final String DEFAULT_ORGANIZATION_ID = "100001";
    // 默认项目ID
    private static final String DEFAULT_PROJECT_ID = "100001100001";
    // 系统成员用户组ID
    private static final String SYSTEM_MEMBER_ROLE_ID = "member";
    // 数据库配置键名
    private static final String LARK_AGENT_ID_KEY = "lark.agent.id";
    private static final String LARK_APP_SECRET_KEY = "lark.app.secret";
    private static final String LARK_REDIRECT_URI_KEY = "lark.redirect.uri";
    private static final String LARK_ENABLED_KEY = "lark.enabled";
    private static final String LARK_VALID_KEY = "lark.valid";
    @Resource
    private UserMapper userMapper;
    @Resource
    private UserRoleRelationMapper userRoleRelationMapper;
    @Resource
    private UserLoginService userLoginService;
    @Resource
    private UserXpackService userXpackService;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private SystemParameterMapper systemParameterMapper;
    // 飞书应用配置
    @Value("${lark.app.id:}")
    private String larkAppId;
    @Value("${lark.app.secret:}")
    private String larkAppSecret;
    @Value("${lark.redirect.uri:}")
    private String larkRedirectUri;

    @Override
    public SessionUser handleCallback(HttpServletRequest request) {
        try {
            String code = request.getParameter("code");
            String state = request.getParameter("state");
            String error = request.getParameter("error");

            // 检查是否有错误
            if (StringUtils.isNotBlank(error)) {
                throw new MSException("用户拒绝授权");
            }

            if (StringUtils.isBlank(code)) {
                throw new MSException("缺少授权码");
            }

            // 验证state参数（可选，用于防止CSRF攻击）
            // 支持快捷登录和二维码登录两种方式
            // 支持在state参数中添加应用标识，格式：fit2cloud-lark-quick-{appId}
            if (StringUtils.isNotBlank(state)) {
                boolean isValidState = "fit2cloud-lark-qr".equals(state) ||
                        "fit2cloud-lark-quick".equals(state) ||
                        state.startsWith("fit2cloud-lark-quick-") ||
                        state.startsWith("fit2cloud-lark-qr-");
                if (!isValidState) {
                throw new MSException("授权状态验证失败");
                }
            }

            return login(code, state);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public SessionUser login(String code, String state) {
        try {
            // 首先检查飞书登录是否启用
            if (!isLarkLoginEnabled()) {
                throw new MSException("飞书登录功能未启用，请联系管理员");
            }

            // 1. 通过授权码获取访问令牌
            String accessToken = getAccessToken(code);
            if (StringUtils.isBlank(accessToken)) {
                throw new MSException("获取飞书访问令牌失败，请检查应用配置");
            }

            // 2. 获取用户信息
            LarkUserInfo larkUserInfo = getUserInfoByAccessToken(accessToken);
            if (larkUserInfo == null) {
                throw new MSException("获取飞书用户信息失败，请重试");
            }

            // 3. 查找或创建用户
            User user = findOrCreateUser(larkUserInfo);
            if (user == null) {
                throw new MSException("用户创建失败");
            }

            // 4. 创建会话用户
            UserDTO userDTO = userLoginService.getUserDTO(user.getId());
            if (userDTO == null) {
                throw new MSException("获取用户信息失败");
            }

            // 5. 自动切换用户资源
            try {
                userLoginService.autoSwitch(userDTO);
            } catch (Exception e) {
                // 不抛出异常，继续执行
            }

            // 6. 创建会话用户
            SessionUser sessionUser = SessionUser.fromUser(userDTO, SessionUtils.getSessionId());
            if (sessionUser == null) {
                throw new MSException("创建会话用户失败");
            }

            // 7. 设置Shiro认证状态
            try {
                Subject subject = SecurityUtils.getSubject();
                Session session = subject.getSession();

                System.out.println("设置Shiro Session - SessionId: " + session.getId());
                System.out.println("设置Shiro Session - 用户: " + sessionUser.getName());

                // 设置认证来源为LARK
                session.setAttribute("authenticate", UserSource.LARK.name());

                // 设置session用户信息
                session.setAttribute(SessionConstants.ATTR_USER, sessionUser);

                // 设置session过期时间
                session.setTimeout(604800000); // 7天

                // 确保session持久化
                session.setAttribute(org.springframework.session.FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, sessionUser.getId());
                
                if (!subject.isAuthenticated()) {
                    // 创建一个认证令牌，使用用户ID作为用户名，使用邮箱作为密码（用于Shiro验证）
                    String password = larkUserInfo.getEmail(); 
                    UsernamePasswordToken token = new UsernamePasswordToken(user.getId(), password);
                    subject.login(token);
                    System.out.println("Shiro认证完成 - 用户已认证: " + subject.isAuthenticated());
                }

                // 强制session立即持久化
                session.touch();
                System.out.println("Session设置完成 - 强制持久化");

            } catch (Exception e) {
                System.out.println("Shiro认证失败: " + e.getMessage());
                e.printStackTrace();
                throw new MSException("Shiro认证失败: " + e.getMessage());
            }

            return sessionUser;

        } catch (MSException e) {
            throw e;
        } catch (Exception e) {
            throw new MSException("飞书登录失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getLarkInfo() {
        Map<String, Object> info = new HashMap<>();

        // 检查飞书登录是否启用
        boolean isEnabled = isLarkLoginEnabled();

        // 检查是否有前端配置
        String frontendAppId = getFrontendConfigAppId();
        String frontendAppSecret = getFrontendConfigAppSecret();
        String frontendRedirectUri = getFrontendConfigRedirectUri();

        if (StringUtils.isNotBlank(frontendAppId) && StringUtils.isNotBlank(frontendAppSecret)) {
            // 使用前端配置
            info.put("agentId", frontendAppId);
            info.put("callBack", frontendRedirectUri);
            info.put("appSecret", frontendAppSecret);
            info.put("useFrontendConfig", true);
            info.put("configSource", "frontend");
        } else {
            // 使用默认配置
            info.put("agentId", larkAppId);
            info.put("callBack", larkRedirectUri);
            info.put("appSecret", larkAppSecret);
            info.put("useFrontendConfig", false);
            info.put("configSource", "default");
        }

        // 前端期望的字段
        info.put("enable", isEnabled);

        // 判断配置是否有效
        boolean isValid = (StringUtils.isNotBlank((String) info.get("agentId")) &&
                StringUtils.isNotBlank((String) info.get("appSecret")));
        info.put("valid", isValid);
        info.put("hasConfig", isValid);

        return info;
    }

    /**
     * 保存到数据库
     */
    private void saveToDatabase(String key, String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }

        SystemParameter param = systemParameterMapper.selectByPrimaryKey(key);
        if (param != null) {
            param.setParamValue(value);
            systemParameterMapper.updateByPrimaryKey(param);
        } else {
            param = new SystemParameter();
            param.setParamKey(key);
            param.setParamValue(value);
            param.setType("TEXT");
            systemParameterMapper.insert(param);
        }
    }

    /**
     * 从数据库读取
     */
    private String getFromDatabase(String key) {
        SystemParameter param = systemParameterMapper.selectByPrimaryKey(key);
        return param != null ? param.getParamValue() : null;
    }

    /**
     * 删除数据库中的配置
     */
    private void deleteFromDatabase(String key) {
        SystemParameter param = systemParameterMapper.selectByPrimaryKey(key);
        if (param != null) {
            systemParameterMapper.deleteByPrimaryKey(key);
        }
    }

    /**
     * 获取前端配置的应用ID
     */
    private String getFrontendConfigAppId() {
        return getFromDatabase(LARK_AGENT_ID_KEY);
    }

    /**
     * 获取前端配置的应用密钥
     */
    private String getFrontendConfigAppSecret() {
        return getFromDatabase(LARK_APP_SECRET_KEY);
    }

    /**
     * 获取前端配置的回调地址
     */
    private String getFrontendConfigRedirectUri() {
        return getFromDatabase(LARK_REDIRECT_URI_KEY);
    }

    /**
     * 检查飞书登录是否启用
     */
    private boolean isLarkLoginEnabled() {
        String enabled = getFromDatabase(LARK_ENABLED_KEY);
        if (StringUtils.isNotBlank(enabled)) {
            return Boolean.parseBoolean(enabled);
        }
        // 默认启用
        return true;
    }

    /**
     * 获取当前使用的配置来源
     */
    private String getCurrentConfigSource() {
        String frontendAppId = getFrontendConfigAppId();
        String frontendAppSecret = getFrontendConfigAppSecret();

        if (StringUtils.isNotBlank(frontendAppId) && StringUtils.isNotBlank(frontendAppSecret)) {
            return "frontend";
        } else {
            return "default";
        }
    }

    /**
     * 获取前端配置的启用状态
     */
    private String getFrontendConfigEnabled() {
        return getFromDatabase(LARK_ENABLED_KEY);
    }

    @Override
    public Map<String, Object> saveConfig(Map<String, Object> config) {
        try {
            // 获取前端配置参数
            String agentId = (String) config.get("agentId");
            String appSecret = (String) config.get("appSecret");
            String callBack = (String) config.get("callBack");
            Boolean enable = (Boolean) config.get("enable");
            Boolean valid = (Boolean) config.get("valid");


            // 保存到数据库
            if (agentId != null) {
                saveToDatabase(LARK_AGENT_ID_KEY, agentId);
            }
            if (appSecret != null) {
                saveToDatabase(LARK_APP_SECRET_KEY, appSecret);
            }
            if (callBack != null) {
                saveToDatabase(LARK_REDIRECT_URI_KEY, callBack);
            }
            if (enable != null) {
                saveToDatabase(LARK_ENABLED_KEY, enable.toString());
            }
            if (valid != null) {
                saveToDatabase(LARK_VALID_KEY, valid.toString());
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "配置保存成功");
            result.put("config", config);
            result.put("enable", enable);

            // 计算配置有效性
            String currentAgentId = getFrontendConfigAppId() != null ? getFrontendConfigAppId() : larkAppId;
            String currentAppSecret = getFrontendConfigAppSecret() != null ? getFrontendConfigAppSecret() : larkAppSecret;
            boolean calculatedValid = StringUtils.isNotBlank(currentAgentId) &&
                    StringUtils.isNotBlank(currentAppSecret);
            result.put("valid", calculatedValid);
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "配置保存失败: " + e.getMessage());
            return result;
        }
    }

    @Override
    public Map<String, Object> testConnection(Map<String, Object> config) {
        try {
            // 获取飞书应用配置
            String agentId = (String) config.get("agentId");
            String appSecret = (String) config.get("appSecret");

            if (StringUtils.isBlank(agentId) || StringUtils.isBlank(appSecret)) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "应用ID和应用密钥不能为空");
                return result;
            }

            // 测试飞书API连接 - 使用tenant_access_token接口进行测试
            String url = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("app_id", agentId);
            requestBody.put("app_secret", appSecret);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            Map<String, Object> result = new HashMap<>();

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> jsonResponse = JSON.parseMap(response.getBody());
                if (Integer.valueOf(0).equals(jsonResponse.get("code"))) {
                    result.put("success", true);
                    result.put("message", "飞书连接测试成功");
                    result.put("accessToken", jsonResponse.get("tenant_access_token"));
                } else {
                    result.put("success", false);
                    result.put("message", "飞书连接测试失败: " + jsonResponse.get("msg"));
                }
            } else {
                result.put("success", false);
                result.put("message", "飞书连接测试失败: HTTP状态码 " + response.getStatusCode());
            }

            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "测试飞书连接异常: " + e.getMessage());
            return result;
        }
    }

    /**
     * 通过授权码获取访问令牌
     */
    private String getAccessToken(String code) {
        try {
            // 首先检查飞书登录是否启用
            if (!isLarkLoginEnabled()) {
                return null;
            }

            // 获取当前使用的配置（前端配置或默认配置）
            String currentAppId = getCurrentAppId();
            String currentAppSecret = getCurrentAppSecret();
            String currentRedirectUri = getCurrentRedirectUri();

            if (StringUtils.isBlank(currentAppId) || StringUtils.isBlank(currentAppSecret)) {
                return null;
            }

            // 使用新版OAuth2.0 API
            String url = "https://open.feishu.cn/open-apis/authen/v2/oauth/token";

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("grant_type", "authorization_code");
            requestBody.put("client_id", currentAppId);
            requestBody.put("client_secret", currentAppSecret);
            requestBody.put("code", code);
            requestBody.put("redirect_uri", currentRedirectUri);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> jsonResponse = JSON.parseMap(response.getBody());
                if (Integer.valueOf(0).equals(jsonResponse.get("code"))) {
                    // 新版API返回的是access_token而不是tenant_access_token
                    String accessToken = (String) jsonResponse.get("access_token");
                    return accessToken;
                } else {
                    // 根据官方文档处理错误码
                    Integer errorCode = (Integer) jsonResponse.get("code");
                    String errorMsg = (String) jsonResponse.get("error_description");

                    // 根据错误码提供具体的错误信息
                    switch (errorCode) {
                        case 20001:
                            throw new MSException("请求缺少必要参数");
                        case 20002:
                            throw new MSException("应用认证失败，请检查App ID和App Secret");
                        case 20003:
                            throw new MSException("授权码无效或已被使用");
                        case 20004:
                            throw new MSException("授权码已过期，请重新授权");
                        case 20008:
                            throw new MSException("用户不存在");
                        case 20009:
                            throw new MSException("租户未安装应用");
                        case 20010:
                            throw new MSException("用户无应用使用权限");
                        case 20071:
                            throw new MSException("重定向URI不匹配");
                        default:
                            throw new MSException("获取访问令牌失败: " + errorMsg);
                    }
                }
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取当前使用的应用ID
     */
    private String getCurrentAppId() {
        if (!isLarkLoginEnabled()) {
            return null;
        }

        String frontendAgentId = getFrontendConfigAppId();
        return StringUtils.isNotBlank(frontendAgentId) ? frontendAgentId : larkAppId;
    }

    /**
     * 获取当前使用的应用密钥
     */
    private String getCurrentAppSecret() {
        if (!isLarkLoginEnabled()) {
            return null;
        }

        String frontendAppSecret = getFrontendConfigAppSecret();
        return StringUtils.isNotBlank(frontendAppSecret) ? frontendAppSecret : larkAppSecret;
    }

    /**
     * 获取当前使用的回调地址
     */
    private String getCurrentRedirectUri() {
        if (!isLarkLoginEnabled()) {
            return null;
        }

        String frontendRedirectUri = getFrontendConfigRedirectUri();
        return StringUtils.isNotBlank(frontendRedirectUri) ? frontendRedirectUri : larkRedirectUri;
    }

    /**
     * 获取用户信息
     */
    private LarkUserInfo getUserInfo(String code) {
        try {
            // 首先通过授权码获取user_access_token
            String userAccessToken = getAccessToken(code);
            if (StringUtils.isBlank(userAccessToken)) {
                return null;
            }

            // 使用user_access_token获取用户信息
            return getUserInfoByAccessToken(userAccessToken);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 通过user_access_token获取用户详细信息
     */
    private LarkUserInfo getUserInfoByAccessToken(String userAccessToken) {
        try {
            String url = "https://open.feishu.cn/open-apis/authen/v1/user_info";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + userAccessToken);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> jsonResponse = JSON.parseMap(response.getBody());
                if (Integer.valueOf(0).equals(jsonResponse.get("code"))) {
                    Map<String, Object> data = (Map<String, Object>) jsonResponse.get("data");

                    LarkUserInfo userInfo = new LarkUserInfo();
                    userInfo.setOpenId((String) data.get("open_id"));
                    userInfo.setName((String) data.get("name"));
                    userInfo.setEmail((String) data.get("email"));
                    userInfo.setAvatar((String) data.get("avatar_url"));
                    userInfo.setMobile((String) data.get("mobile"));

                    return userInfo;
                } else {
                    // 根据官方文档处理错误码
                    Integer errorCode = (Integer) jsonResponse.get("code");
                    String errorMsg = (String) jsonResponse.get("msg");

                    // 根据错误码提供具体的错误信息
                    switch (errorCode) {
                        case 20001:
                            throw new MSException("请求参数错误");
                        case 20005:
                            throw new MSException("访问令牌无效");
                        case 20008:
                            throw new MSException("用户不存在");
                        case 20021:
                            throw new MSException("用户已离职");
                        case 20022:
                            throw new MSException("用户被冻结");
                        case 20023:
                            throw new MSException("用户未完成注册");
                        default:
                            throw new MSException("获取用户信息失败: " + errorMsg);
                    }
                }
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 查找或创建用户
     * 逻辑：
     * 1. 首先通过飞书open_id查找用户，如果找到则直接返回
     * 2. 如果没找到，通过邮箱查找用户
     * 3. 如果通过邮箱找到用户，检查该用户是否已有飞书ID
     *    - 如果没有飞书ID，则更新用户的飞书ID
     *    - 如果已有飞书ID，则返回该用户
     * 4. 如果都没找到，创建新用户
     */
    private User findOrCreateUser(LarkUserInfo larkUserInfo) {
        // 1. 通过飞书open_id查找用户
        User user = findUserByLarkOpenId(larkUserInfo.getOpenId());
        if (user != null) {
            return user;
        }

        // 2. 通过邮箱查找用户
        if (StringUtils.isNotBlank(larkUserInfo.getEmail())) {
            user = findUserByEmail(larkUserInfo.getEmail());
            if (user != null) {
                // 检查该用户是否已有飞书ID
                if (StringUtils.isBlank(user.getLarkOpenId())) {
                    // 用户没有飞书ID，更新用户的飞书ID
                    updateUserLarkOpenId(user.getId(), larkUserInfo.getOpenId());
                } else if (!larkUserInfo.getOpenId().equals(user.getLarkOpenId())) {
                    // 用户已有飞书ID，但ID不匹配，记录警告但继续使用现有用户
                }
                return user;
            }
        }

        // 3. 创建新用户
        return createLarkUser(larkUserInfo);
    }

    /**
     * 通过飞书open_id查找用户
     */
    private User findUserByLarkOpenId(String openId) {
        if (StringUtils.isBlank(openId)) {
            return null;
        }

        UserExample example = new UserExample();
        example.createCriteria()
                .andLarkOpenIdEqualTo(openId)
                .andDeletedEqualTo(false);

        List<User> users = userMapper.selectByExample(example);
        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * 通过邮箱查找用户
     */
    private User findUserByEmail(String email) {
        if (StringUtils.isBlank(email)) {
            return null;
        }

        UserExample example = new UserExample();
        example.createCriteria()
                .andEmailEqualTo(email)
                .andDeletedEqualTo(false);

        List<User> users = userMapper.selectByExample(example);
        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * 更新用户的飞书open_id
     */
    private void updateUserLarkOpenId(String userId, String openId) {
        if (StringUtils.isBlank(userId) || StringUtils.isBlank(openId)) {
            return;
        }

        User user = new User();
        user.setId(userId);
        user.setLarkOpenId(openId);
        user.setUpdateTime(System.currentTimeMillis());
        user.setUpdateUser(userId);

        userMapper.updateByPrimaryKeySelective(user);
    }

    /**
     * 创建飞书用户
     * 严格按照正常新建用户的流程，确保字段设置完全一致
     */
    private User createLarkUser(LarkUserInfo larkUserInfo) {
        try {

            User user = new User();
            user.setId(IDGenerator.nextStr());
            user.setName(larkUserInfo.getName()); // 使用飞书返回的用户名
            user.setEmail(larkUserInfo.getEmail());
            user.setLarkOpenId(larkUserInfo.getOpenId());
            // 设置默认密码为邮箱的MD5值，与正常新建用户保持一致
            user.setPassword(CodingUtils.md5(larkUserInfo.getEmail()));
            user.setEnable(true);

            // 设置时间戳，确保creationTime不为null，与正常新建用户保持一致
            long currentTime = System.currentTimeMillis();
            user.setCreateTime(currentTime);
            user.setUpdateTime(currentTime);
            user.setCreateUser(user.getId());
            user.setUpdateUser(user.getId());
            user.setDeleted(false);

            // 设置来源为LARK，与正常新建用户保持一致
            user.setSource(UserSource.LARK.name());
            // 设置默认语言为zh-CN，与正常新建用户保持一致
            user.setLanguage("zh-CN");
            // 设置默认组织ID，与正常新建用户保持一致
            user.setLastOrganizationId(DEFAULT_ORGANIZATION_ID);
            // 设置默认项目ID，与正常新建用户保持一致
            user.setLastProjectId(DEFAULT_PROJECT_ID);

            // 生成cft_token - 严格按照UserXpackServiceImpl的模式
            String cftToken;
            try {
                cftToken = generateCftToken(user.getId(), currentTime);
                user.setCftToken(cftToken);
            } catch (Exception e) {
                throw new MSException("创建飞书用户失败：无法生成cft_token");
            }

            userMapper.insert(user);

            try {
                assignDefaultRoles(user.getId());
            } catch (Exception e) {
                // 不抛出异常，用户创建成功，角色分配失败不影响登录
            }

            return user;
        } catch (Exception e) {
            throw new MSException("创建飞书用户失败: " + e.getMessage());
        }
    }

    /**
     * 分配默认角色
     */
    private void assignDefaultRoles(String userId) {
        try {
            // 系统成员角色
            UserRoleRelation memberRelation = new UserRoleRelation();
            memberRelation.setId(IDGenerator.nextStr());
            memberRelation.setUserId(userId);
            memberRelation.setRoleId(SYSTEM_MEMBER_ROLE_ID);
            memberRelation.setSourceId("system");
            memberRelation.setOrganizationId("system");
            memberRelation.setCreateTime(System.currentTimeMillis());
            memberRelation.setCreateUser("system");
            userRoleRelationMapper.insert(memberRelation);

            // 组织成员角色
            UserRoleRelation orgMemberRelation = new UserRoleRelation();
            orgMemberRelation.setId(IDGenerator.nextStr());
            orgMemberRelation.setUserId(userId);
            orgMemberRelation.setRoleId("org_member");
            orgMemberRelation.setSourceId(DEFAULT_ORGANIZATION_ID);
            orgMemberRelation.setOrganizationId(DEFAULT_ORGANIZATION_ID);
            orgMemberRelation.setCreateTime(System.currentTimeMillis());
            orgMemberRelation.setCreateUser("system");
            userRoleRelationMapper.insert(orgMemberRelation);

            // 项目成员角色
            UserRoleRelation projectMemberRelation = new UserRoleRelation();
            projectMemberRelation.setId(IDGenerator.nextStr());
            projectMemberRelation.setUserId(userId);
            projectMemberRelation.setRoleId("project_member");
            projectMemberRelation.setSourceId(DEFAULT_PROJECT_ID);
            projectMemberRelation.setOrganizationId(DEFAULT_ORGANIZATION_ID);
            projectMemberRelation.setCreateTime(System.currentTimeMillis());
            projectMemberRelation.setCreateUser("system");
            userRoleRelationMapper.insert(projectMemberRelation);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * 飞书用户信息
     */
    private static class LarkUserInfo {
        private String openId;
        private String name;
        private String email;
        private String avatar;
        private String mobile;

        public String getOpenId() {
            return openId;
        }

        public void setOpenId(String openId) {
            this.openId = openId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getAvatar() {
            return avatar;
        }

        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
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

    @Override
    public void clearFrontendConfig() {
        try {
            // 清除数据库中的前端配置
            deleteFromDatabase(LARK_AGENT_ID_KEY);
            deleteFromDatabase(LARK_APP_SECRET_KEY);
            deleteFromDatabase(LARK_REDIRECT_URI_KEY);
            deleteFromDatabase(LARK_ENABLED_KEY);
            deleteFromDatabase(LARK_VALID_KEY);
        } catch (Exception e) {
            throw new MSException("清除配置失败: " + e.getMessage());
        }
    }
}
