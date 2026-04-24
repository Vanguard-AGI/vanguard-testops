package io.vanguard.testops.system.service;

import io.vanguard.testops.system.dto.sdk.SessionUser;
import io.vanguard.testops.system.service.impl.LarkSSOServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.annotation.Resource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 飞书SSO服务测试
 */
@SpringBootTest
@ActiveProfiles("test")
public class LarkSSOServiceTests {

    @Resource
    private LarkSSOService larkSSOService;

    @Test
    public void testGetLarkInfo() {
        Map<String, Object> info = larkSSOService.getLarkInfo();
        assertNotNull(info);
        assertTrue(info.containsKey("appId"));
        assertTrue(info.containsKey("redirectUri"));
        assertTrue(info.containsKey("enabled"));
    }

    @Test
    public void testLarkLoginWithInvalidCode() {
        // 测试无效的授权码
        assertThrows(Exception.class, () -> {
            larkSSOService.login("invalid_code", "test_state");
        });
    }
}
