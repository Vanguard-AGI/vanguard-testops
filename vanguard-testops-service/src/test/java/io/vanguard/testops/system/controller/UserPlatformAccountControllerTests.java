package io.vanguard.testops.system.controller;

import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.system.base.BasePluginTestService;
import io.vanguard.testops.system.base.BaseTest;
import io.vanguard.testops.system.controller.handler.ResultHolder;
import io.vanguard.testops.system.domain.Plugin;
import io.vanguard.testops.system.domain.UserExtend;
import io.vanguard.testops.system.domain.UserExtendExample;
import io.vanguard.testops.system.mapper.UserExtendMapper;
import io.vanguard.testops.system.service.UserPlatformAccountService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.Header;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserPlatformAccountControllerTests extends BaseTest {

    @Resource
    private BasePluginTestService basePluginTestService;
    @Value("${embedded.mockserver.host}")
    private String mockServerHost;
    @Value("${embedded.mockserver.port}")
    private int mockServerHostPort;
    @Resource
    private MockServerClient mockServerClient;
    @Resource
    private UserExtendMapper userExtendMapper;
    @Resource
    private UserPlatformAccountService userPlatformAccountService;
    private static final String VALIDATE_POST = "/user/platform/validate/{0}/{1}";
    private static final String SAVE_POST = "/user/platform/save";
    public static <T> T parseObjectFromMvcResult(MvcResult mvcResult, Class<T> parseClass) {
        try {
            String returnData = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
            ResultHolder resultHolder = JSON.parseObject(returnData, ResultHolder.class);
            //返回请求正常
            Assertions.assertNotNull(resultHolder);
            return JSON.parseObject(JSON.toJSONString(resultHolder.getData()), parseClass);
        } catch (Exception ignore) {
        }
        return null;
    }

    @Test
    @Order(1)
    public void testGetAccountInfoList() throws Exception {
        basePluginTestService.getJiraPlugin();
        MvcResult mvcResult = this.requestGetAndReturn("/user/platform/account/info");
        Map<String, Object> accountMap = parseObjectFromMvcResult(mvcResult, Map.class);
        Assertions.assertNotNull(accountMap);
    }

    @Test
    @Order(2)
    public void validatePost() throws Exception {
        mockServerClient
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/rest/api/2/myself"))
                .respond(
                        response()
                                .withStatusCode(200)
                                .withHeaders(
                                        new Header("Content-Type", "application/json; charset=utf-8"),
                                        new Header("Cache-Control", "public, max-age=86400"))
                                .withBody("{\"self\"")
                );
        Plugin plugin = basePluginTestService.getJiraPlugin();
        BasePluginTestService.JiraIntegrationConfig integrationConfig = new BasePluginTestService.JiraIntegrationConfig();
        integrationConfig.setAddress(String.format("http://%s:%s", mockServerHost, mockServerHostPort));
        Map<String, Object> integrationConfigMap = JSON.parseMap(JSON.toJSONString(integrationConfig));
    }

    @Test
    @Order(3)
    public void testSave() throws Exception {
        // 未保存用户配置信息, 为空
        MvcResult mvcResult = this.requestGetAndReturn("/user/platform/get/100001");
        // noinspection unchecked
        Map<String, Object> accountMap = parseObjectFromMvcResult(mvcResult, Map.class);
        Assertions.assertNull(accountMap);
        userPlatformAccountService.getPluginUserPlatformConfig("jira", "100001", "admin");
        // @@请求成功 保存两次
        this.requestPostWithOk(SAVE_POST, buildUserPlatformConfig());
        UserExtend record = new UserExtend();
        record.setId("admin");
        record.setPlatformInfo(null);
        userExtendMapper.updateByPrimaryKeyWithBLOBs(record);
        this.requestPostWithOk(SAVE_POST, buildUserPlatformConfig());
        this.requestPostWithOk(SAVE_POST, buildUserPlatformConfig());
        userPlatformAccountService.getPluginUserPlatformConfig("jira", "100001", "admin");
    }

    @Test
    @Order(4)
    public void testGet() throws Exception {
        MvcResult mvcResult = this.requestGetAndReturn("/user/platform/get/100001");
        // noinspection unchecked
        Map<String, Object> accountMap = parseObjectFromMvcResult(mvcResult, Map.class);
        Assertions.assertNotNull(accountMap);
        // 删除用户配置信息, 避免影响后续测试
        UserExtendExample example = new UserExtendExample();
        example.createCriteria().andIdEqualTo("admin");
        userExtendMapper.deleteByExample(example);
    }

    @Test
    @Order(5)
    public void testGetPlatformOption() throws Exception {
        this.requestGet("/user/platform/switch-option");
    }

    private Map<String, Object> buildUserPlatformConfig() {
        Map<String, Object> platformInfo = new HashMap<>();
        Map<String, Object> userPlatformConfig = new HashMap<>();
        Map<String, Object> zentaoConfig = new HashMap<>();
        Map<String, Object> jiraConfig = new HashMap<>();
        zentaoConfig.put("zentaoAccount", "test");
        zentaoConfig.put("zentaoPassword", "test");
        jiraConfig.put("authType", "test");
        jiraConfig.put("jiraAccount", "test");
        jiraConfig.put("jiraPassword", "test");
        userPlatformConfig.put("zentao", zentaoConfig);
        userPlatformConfig.put("jira", jiraConfig);
        platformInfo.put("100001", userPlatformConfig);
        return platformInfo;
    }
}
