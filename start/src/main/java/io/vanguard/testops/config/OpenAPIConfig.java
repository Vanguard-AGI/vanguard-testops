package io.vanguard.testops.config;

import io.vanguard.testops.sdk.constants.SessionConstants;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.security.SecurityRequirement; // Added import

/**
 * OpenAPI/Swagger 配置类
 * 为 Swagger UI 添加认证支持
 */
@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        // 定义安全方案
        SecurityScheme authTokenScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name(SessionConstants.HEADER_TOKEN)
                .description("认证 Token，从登录接口获取的 sessionId");

        SecurityScheme csrfTokenScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name(SessionConstants.CSRF_TOKEN)
                .description("CSRF Token，从登录接口获取的 csrfToken");

        // 创建安全方案组件
        Components components = new Components()
                .addSecuritySchemes("X-AUTH-TOKEN", authTokenScheme)
                .addSecuritySchemes("CSRF-TOKEN", csrfTokenScheme);

        // 创建全局安全要求（可选，如果需要所有接口都要求认证，可以取消注释）
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("X-AUTH-TOKEN")
                .addList("CSRF-TOKEN");

        return new OpenAPI()
                .components(components)
                .addSecurityItem(securityRequirement) // 如果需要全局认证，取消注释
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("MeterSphere API 文档")
                        .version("3.x")
                        .description("MeterSphere 测试工厂 API 文档"));
    }
}

