package io.vanguard.testops.project.controller;

import io.vanguard.testops.project.domain.Project;
import io.vanguard.testops.project.mapper.ProjectMapper;
import io.vanguard.testops.project.service.CreateEnvironmentResourceService;
import io.vanguard.testops.sdk.domain.Environment;
import io.vanguard.testops.sdk.domain.EnvironmentBlob;
import io.vanguard.testops.sdk.domain.EnvironmentExample;
import io.vanguard.testops.sdk.mapper.EnvironmentBlobMapper;
import io.vanguard.testops.sdk.mapper.EnvironmentMapper;
import io.vanguard.testops.system.base.BaseTest;
import io.vanguard.testops.system.uid.IDGenerator;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;


@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@AutoConfigureMockMvc
public class CreateEnvironmentTests extends BaseTest {
    @Resource
    private CreateEnvironmentResourceService createEnvironmentResourceService;
    @Resource
    private EnvironmentMapper environmentMapper;
    @Resource
    private EnvironmentBlobMapper environmentBlobMapper;
    @Resource
    private ProjectMapper projectMapper;

    @Test
    @Order(1)
    public void testCreateResource() throws Exception {
        Project initProject = new Project();
        initProject.setId(IDGenerator.nextStr());
        initProject.setNum(null);
        initProject.setOrganizationId("100001");
        initProject.setName("测试生成mock环境");
        initProject.setDescription("测试生成mock环境");
        initProject.setCreateUser("admin");
        initProject.setUpdateUser("admin");
        initProject.setCreateTime(System.currentTimeMillis());
        initProject.setUpdateTime(System.currentTimeMillis());
        initProject.setEnable(true);
        initProject.setModuleSetting("[\"apiTest\",\"uiTest\"]");
        projectMapper.insertSelective(initProject);
        createEnvironmentResourceService.createResources(initProject.getId());
        EnvironmentExample environmentExample = new EnvironmentExample();
        environmentExample.createCriteria().andProjectIdEqualTo(initProject.getId()).andNameEqualTo("Mock环境");
        List<Environment> environments = environmentMapper.selectByExample(environmentExample);
        assert environments.size() == 1;
        EnvironmentBlob environmentBlob = environmentBlobMapper.selectByPrimaryKey(environments.getFirst().getId());
        assert environmentBlob != null;
    }

}
