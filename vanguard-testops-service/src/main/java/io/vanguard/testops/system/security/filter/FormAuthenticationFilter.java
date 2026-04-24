package io.vanguard.testops.system.security.filter;

import io.vanguard.testops.sdk.constants.SessionConstants;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * 自定义表单认证过滤器
 * 当 API 请求认证失败时，返回 JSON 而不是重定向到 HTML
 */
public class FormAuthenticationFilter extends org.apache.shiro.web.filter.authc.FormAuthenticationFilter {

    @Override
    protected void redirectToLogin(ServletRequest request, ServletResponse response) throws IOException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 检查是否是 API 请求（通过 Accept 头、Content-Type 头或路径判断）
        String accept = httpRequest.getHeader("Accept");
        String contentType = httpRequest.getHeader("Content-Type");
        String path = httpRequest.getRequestURI();

        // 如果是 API 请求，返回 JSON
        // 判断条件：
        // 1. Accept 头包含 application/json
        // 2. Content-Type 是 application/json
        // 3. 路径是 API 路径（/api/, /metadata/, /workflow/, /test-factory/ 等）
        // 4. 不是静态资源路径（.html, .js, .css 等）
        boolean isApiRequest = (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE)) ||
            (contentType != null && contentType.contains(MediaType.APPLICATION_JSON_VALUE)) ||
            path.startsWith("/api/") ||
            path.startsWith("/metadata/") ||
            path.startsWith("/workflow/") ||
            path.startsWith("/test-factory/") ||
            (!path.contains(".") && !path.equals("/") && !path.equals("/login")); // 没有文件扩展名且不是根路径或登录页

        if (isApiRequest) {
            
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            httpResponse.setCharacterEncoding("UTF-8");
            httpResponse.setHeader(SessionConstants.AUTHENTICATION_STATUS, SessionConstants.AUTHENTICATION_INVALID);
            
            PrintWriter writer = httpResponse.getWriter();
            writer.write("{\"code\":401,\"message\":\"用户认证失败，请先登录\",\"data\":null}");
            writer.flush();
            return;
        }

        // 否则，执行默认的重定向行为
        super.redirectToLogin(request, response);
    }
}

