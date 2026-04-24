package io.vanguard.testops.system.controller;
import io.vanguard.testops.system.controller.handler.ResultHolder;
import io.vanguard.testops.system.controller.handler.result.MsHttpResultCode;
import io.vanguard.testops.system.dto.sdk.SessionUser;
import io.vanguard.testops.system.service.LarkSSOService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.SecurityUtils;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

import static io.vanguard.testops.sdk.constants.SessionConstants.ATTR_USER;

@RestController
@Tag(name = "SSO单点登录")
@CrossOrigin(origins = {"http://aegis.tst.spotter.ink", "https://aegis.tst.spotter.ink"}, allowCredentials = "true")
public class SSOController {


    @Resource
    private LarkSSOService larkSSOService;

    @Resource
    private RedisIndexedSessionRepository redisIndexedSessionRepository;

    // ==================== 飞书登录相关接口 ====================

    @RequestMapping(value = "/devops/feishu/callback", method = {RequestMethod.GET, RequestMethod.POST})
    @Operation(summary = "飞书登录回调")
    public void larkCallback(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            // 处理飞书登录回调
            System.out.println("开始处理飞书回调请求");
            SessionUser sessionUser = larkSSOService.handleCallback(request);
            System.out.println("飞书回调处理完成，sessionUser: " + (sessionUser != null ? sessionUser.getName() : "null"));

            if (sessionUser != null) {
                // 登录成功，确保session正确设置
                org.apache.shiro.SecurityUtils.getSubject().getSession().setAttribute("authenticate", io.vanguard.testops.sdk.constants.UserSource.LARK.name());
                org.apache.shiro.SecurityUtils.getSubject().getSession().setAttribute(io.vanguard.testops.sdk.constants.SessionConstants.ATTR_USER, sessionUser);

                // 确保session持久化
                org.apache.shiro.SecurityUtils.getSubject().getSession().setAttribute(org.springframework.session.FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, sessionUser.getId());

                // 设置session过期时间（可选）
                org.apache.shiro.SecurityUtils.getSubject().getSession().setTimeout(604800000); // 7天

                // 设置Cookie以确保session持久化
                String sessionId = org.apache.shiro.SecurityUtils.getSubject().getSession().getId().toString();
                jakarta.servlet.http.Cookie sessionCookie = new jakarta.servlet.http.Cookie("JSESSIONID", sessionId);
                sessionCookie.setPath("/");
                sessionCookie.setDomain("aegis.tst.spotter.ink"); // 设置正确的域名
                sessionCookie.setHttpOnly(false); // 允许JavaScript访问
                sessionCookie.setMaxAge(604800); // 7天
                sessionCookie.setSecure(false); // 开发环境不需要HTTPS
                // SameSite属性通过其他方式设置，避免编译错误
                // sessionCookie.setSameSite(jakarta.servlet.http.Cookie.SameSite.LAX);
                response.addCookie(sessionCookie);

                // 添加额外的调试信息
                System.out.println("设置Cookie JSESSIONID: " + sessionId);
                System.out.println("Cookie域名: aegis.tst.spotter.ink");
                System.out.println("Cookie路径: /");

                // 重定向到前端，根据state参数中的应用标识决定重定向URL
                // 从request中获取state参数
                String state = request.getParameter("state");
                String redirectUrl;

                // 如果state参数中包含应用标识（如 fit2cloud-lark-quick-aegis-one-web）
                if (StringUtils.isNotBlank(state) && state.startsWith("fit2cloud-lark-quick-")) {
                    String appId = state.substring("fit2cloud-lark-quick-".length());
                    System.out.println("检测到应用标识: " + appId);
                    
                    // 根据不同的应用标识重定向到不同的前端URL
                    if ("aegis-one-web".equals(appId)) {
                        // aegis-one-web 使用标准路由格式（无hash）
                        redirectUrl = "http://aegis-ones-web.spotter.ink/?menu=test-factory&tab=api&success=true&source=lark&sessionId=" + sessionId + "&timestamp=" + System.currentTimeMillis();
                    } else {
                        // 其他应用或默认使用hash路由格式（spotter-metersphere前端）
                        redirectUrl = "http://aegis.tst.spotter.ink/#/login?success=true&source=lark&sessionId=" + sessionId + "&timestamp=" + System.currentTimeMillis();
                    }
                } else {
                    // 默认使用hash路由格式（spotter-metersphere前端）
                    redirectUrl = "http://aegis.tst.spotter.ink/#/login?success=true&source=lark&sessionId=" + sessionId + "&timestamp=" + System.currentTimeMillis();
                }
                
                System.out.println("重定向URL: " + redirectUrl);
                response.sendRedirect(redirectUrl);
            } else {
                // 登录失败，重定向到登录页面（根据state参数决定重定向URL）
                System.out.println("登录失败");
                String state = request.getParameter("state");
                String errorRedirectUrl = getRedirectUrlByState(state, "error=login_failed&source=lark");
                response.sendRedirect(errorRedirectUrl);
            }
        } catch (Exception e) {
            // 异常处理，重定向到登录页面（根据state参数决定重定向URL）
            System.out.println("飞书登录异常: " + e.getMessage());
            String state = request.getParameter("state");
            String errorRedirectUrl = getRedirectUrlByState(state, "error=login_failed&source=lark&message=" + e.getMessage());
            response.sendRedirect(errorRedirectUrl);
        }
    }
    
    /**
     * 根据state参数中的应用标识获取重定向URL
     */
    private String getRedirectUrlByState(String state, String queryParams) {
        if (StringUtils.isNotBlank(state) && state.startsWith("fit2cloud-lark-quick-")) {
            String appId = state.substring("fit2cloud-lark-quick-".length());
            if ("aegis-one-web".equals(appId)) {
                // aegis-one-web 使用标准路由格式（无hash）
                return "http://aegis-ones-web.spotter.ink/login?" + queryParams;
            }
        }
        // 默认使用hash路由格式（spotter-metersphere前端）
        return "http://aegis.tst.spotter.ink/#/login?" + queryParams;
    }

    @GetMapping("/lark/info")
    @Operation(summary = "获取飞书登录信息")
    public ResultHolder getLarkInfo() {
        return ResultHolder.success(larkSSOService.getLarkInfo());
    }

    @GetMapping("/setting/get/platform/info")
    @Operation(summary = "获取平台信息")
    public ResultHolder getPlatformInfo() {
        // 返回所有平台的配置信息
        java.util.List<java.util.Map<String, Object>> platforms = new java.util.ArrayList<>();

        // 飞书平台
        java.util.Map<String, Object> larkConfig = larkSSOService.getLarkInfo();
        java.util.Map<String, Object> larkPlatform = new java.util.HashMap<>();
        larkPlatform.put("platform", "lark");
        larkPlatform.put("enable", larkConfig.get("enable"));

        // 检查是否有配置：如果有前端配置或默认配置都算有配置
        boolean hasConfig = org.apache.commons.lang3.StringUtils.isNotBlank((String) larkConfig.get("agentId")) &&
                org.apache.commons.lang3.StringUtils.isNotBlank((String) larkConfig.get("appSecret"));

        // valid字段：如果有配置，则为true（不再依赖enable状态）
        larkPlatform.put("valid", hasConfig);
        larkPlatform.put("hasConfig", hasConfig);


        platforms.add(larkPlatform);

        // 国际飞书平台（不支持）
        java.util.Map<String, Object> larkSuitePlatform = new java.util.HashMap<>();
        larkSuitePlatform.put("platform", "lark_suite");
        larkSuitePlatform.put("enable", false);
        larkSuitePlatform.put("valid", false);
        larkSuitePlatform.put("hasConfig", false);
        platforms.add(larkSuitePlatform);

        return ResultHolder.success(platforms);
    }

    @GetMapping("/setting/get/platform/param")
    @Operation(summary = "获取平台参数")
    public ResultHolder getPlatformParam() {
        // 返回平台参数列表，用于登录页面的二维码登录选项
        java.util.List<java.util.Map<String, Object>> platformParams = new java.util.ArrayList<>();

        // 检查飞书登录是否启用
        try {
            java.util.Map<String, Object> larkConfig = larkSSOService.getLarkInfo();
            Boolean isEnabled = (Boolean) larkConfig.get("enable");
            Boolean isValid = (Boolean) larkConfig.get("valid");

            // 如果飞书登录已启用且配置有效，则添加到平台参数列表
            if (isEnabled != null && isEnabled && isValid != null && isValid) {
                java.util.Map<String, Object> larkParam = new java.util.HashMap<>();
                larkParam.put("id", "LARK");
                larkParam.put("name", "飞书");
                platformParams.add(larkParam);
            }
        } catch (Exception e) {
            // 如果获取飞书配置失败，不添加飞书登录选项
            System.out.println("获取飞书配置失败: " + e.getMessage());
        }

        return ResultHolder.success(platformParams);
    }

    @GetMapping("/lark/login")
    @Operation(summary = "飞书登录")
    public ResultHolder larkLogin(@RequestParam("code") String code) {
        try {
            System.out.println("飞书登录接口调用，code: " + code);
            
            // 处理飞书登录
            SessionUser sessionUser = larkSSOService.login(code, "fit2cloud-lark-quick");
            
            if (sessionUser != null) {
                // 设置认证状态
                org.apache.shiro.SecurityUtils.getSubject().getSession().setAttribute("authenticate", io.vanguard.testops.sdk.constants.UserSource.LARK.name());
                org.apache.shiro.SecurityUtils.getSubject().getSession().setAttribute(io.vanguard.testops.sdk.constants.SessionConstants.ATTR_USER, sessionUser);

                // 返回登录成功信息
                return ResultHolder.success(sessionUser);
            } else {
                return ResultHolder.error(MsHttpResultCode.UNAUTHORIZED.getCode(), "飞书登录失败");
            }
        } catch (Exception e) {
            System.out.println("飞书登录失败: " + e.getMessage());

            return ResultHolder.error(MsHttpResultCode.UNAUTHORIZED.getCode(), e.getMessage());
        }
    }

    @GetMapping("/lark/user")
    @Operation(summary = "通过sessionId获取用户信息（Chrome扩展插件使用）")
    public ResultHolder getLarkUserBySessionId(@RequestParam("sessionId") String sessionId) {
        try {
            if (StringUtils.isBlank(sessionId)) {
                return ResultHolder.error(MsHttpResultCode.UNAUTHORIZED.getCode(), "sessionId不能为空");
            }

            org.springframework.session.Session session = redisIndexedSessionRepository.findById(sessionId);
            if (session == null) {
                return ResultHolder.error(MsHttpResultCode.UNAUTHORIZED.getCode(), "session不存在或已过期");
            }

            SessionUser sessionUser = session.getAttribute(ATTR_USER);
            if (sessionUser == null) {
                return ResultHolder.error(MsHttpResultCode.UNAUTHORIZED.getCode(), "用户未登录");
            }

            return ResultHolder.success(sessionUser);
        } catch (Exception e) {
            System.out.println("通过sessionId获取用户信息失败: " + e.getMessage());
            e.printStackTrace();
            return ResultHolder.error(MsHttpResultCode.UNAUTHORIZED.getCode(), e.getMessage());
        }
    }

    // ==================== 飞书配置管理接口 ====================

    @GetMapping("/lark/info/with_detail")
    @Operation(summary = "获取飞书详细信息")
    public ResultHolder getLarkInfoWithDetail() {
        return ResultHolder.success(larkSSOService.getLarkInfo());
    }

    @PostMapping("/lark/save")
    @Operation(summary = "保存飞书配置")
    public ResultHolder saveLarkConfig(@RequestBody Map<String, Object> config) {
        try {
            return ResultHolder.success(larkSSOService.saveConfig(config));
        } catch (Exception e) {
            return ResultHolder.error("保存飞书配置失败: " + e.getMessage(), null);
        }
    }

    @PostMapping("/lark/enable")
    @Operation(summary = "启用/禁用飞书")
    public ResultHolder enableLark(@RequestBody Map<String, Object> request) {
        try {
            Boolean enable = (Boolean) request.get("enable");
            Map<String, Object> config = new java.util.HashMap<>();
            config.put("enable", enable);  // 修正字段名
            return ResultHolder.success(larkSSOService.saveConfig(config));
        } catch (Exception e) {
            return ResultHolder.error("启用/禁用飞书失败: " + e.getMessage(), null);
        }
    }

    @PostMapping("/lark/validate")
    @Operation(summary = "验证飞书配置")
    public ResultHolder validateLarkConfig(@RequestBody Map<String, Object> config) {
        try {
            return ResultHolder.success(larkSSOService.testConnection(config));
        } catch (Exception e) {
            return ResultHolder.error("验证飞书配置失败: " + e.getMessage(), null);
        }
    }

    @PostMapping("/lark/change/validate")
    @Operation(summary = "关闭飞书验证")
    public ResultHolder closeLarkValidate() {
        try {
            Map<String, Object> config = new java.util.HashMap<>();
            config.put("valid", false);
            return ResultHolder.success(larkSSOService.saveConfig(config));
        } catch (Exception e) {
            return ResultHolder.error("关闭飞书验证失败: " + e.getMessage(), null);
        }
    }

    @PostMapping("/lark/clear-config")
    @Operation(summary = "清除飞书前端配置")
    public ResultHolder clearLarkConfig() {
        try {
            // 清除数据库中的前端配置
            larkSSOService.clearFrontendConfig();
            return ResultHolder.success("前端配置已清除，将使用默认配置");
        } catch (Exception e) {
            return ResultHolder.error("清除配置失败: " + e.getMessage(), null);
        }
    }

    // ==================== 测试接口 ====================

    @GetMapping("/devops/feishu/test")
    @Operation(summary = "飞书回调测试接口")
    public ResultHolder testCallback() {
        return ResultHolder.success("飞书回调接口正常工作");
    }

    // ==================== 国际飞书接口（不支持） ====================

    @GetMapping("/lark_suite/info/with_detail")
    @Operation(summary = "获取国际飞书详细信息")
    public ResultHolder getLarkSuiteInfoWithDetail() {
        return ResultHolder.error("不支持国际飞书登录", null);
    }

    @PostMapping("/lark_suite/save")
    @Operation(summary = "保存国际飞书配置")
    public ResultHolder saveLarkSuiteConfig(@RequestBody Map<String, Object> config) {
        System.out.println(config);
        return ResultHolder.error("不支持国际飞书登录", null);
    }

    @PostMapping("/lark_suite/enable")
    @Operation(summary = "启用/禁用国际飞书")
    public ResultHolder enableLarkSuite(@RequestBody Map<String, Object> request) {
        System.out.println(request);
        return ResultHolder.error("不支持国际飞书登录", null);
    }

    @PostMapping("/lark_suite/validate")
    @Operation(summary = "验证国际飞书配置")
    public ResultHolder validateLarkSuiteConfig(@RequestBody Map<String, Object> config) {
        System.out.println(config);
        return ResultHolder.error("不支持国际飞书登录", null);
    }

    @PostMapping("/lark_suite/change/validate")
    @Operation(summary = "关闭国际飞书验证")
    public ResultHolder closeLarkSuiteValidate() {
        return ResultHolder.error("不支持国际飞书登录", null);
    }
}
