package io.vanguard.testops.system.controller;

import io.vanguard.testops.system.controller.handler.ResultHolder;
import io.vanguard.testops.system.service.LarkSSOService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/authentication")
@Tag(name = "认证")
public class AuthenticationController {

    @Resource
    private LarkSSOService larkSSOService;

    @GetMapping("/get-list")
    @Operation(summary = "获取认证方式列表")
    public ResultHolder getAuthenticationList() {
        // 返回认证方式列表
        List<String> authList = new ArrayList<>();

        // 总是包含本地认证
        authList.add("LOCAL");

        // 检查飞书登录是否启用
        try {
            Map<String, Object> larkConfig = larkSSOService.getLarkInfo();
            Boolean isEnabled = (Boolean) larkConfig.get("enable");
            Boolean isValid = (Boolean) larkConfig.get("valid");

            // 如果飞书登录已启用且配置有效，则添加到认证列表
            if (isEnabled != null && isEnabled && isValid != null && isValid) {
                authList.add("LARK");
            }
        } catch (Exception e) {
            // 如果获取飞书配置失败，不添加飞书登录选项
            System.out.println("获取飞书配置失败: " + e.getMessage());
        }
        
        return ResultHolder.success(authList);
    }
}
