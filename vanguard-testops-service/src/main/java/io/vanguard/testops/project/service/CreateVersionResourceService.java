package io.vanguard.testops.project.service;

import io.vanguard.testops.project.domain.ProjectApplication;
import io.vanguard.testops.project.domain.ProjectVersion;
import io.vanguard.testops.project.mapper.ProjectVersionMapper;
import io.vanguard.testops.sdk.constants.InternalUserRole;
import io.vanguard.testops.sdk.constants.ProjectApplicationType;
import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.system.service.CreateProjectResourceService;
import io.vanguard.testops.system.uid.IDGenerator;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 项目创建-初始化版本资源
 *
 * @author Jan
 */
@Component
public class CreateVersionResourceService implements CreateProjectResourceService {

    public static final String DEFAULT_VERSION = "v1.0";
    public static final String DEFAULT_VERSION_STATUS = "open";

    @Resource
    private ProjectVersionMapper projectVersionMapper;
    @Resource
    private ProjectApplicationService projectApplicationService;

    @Override
    public void createResources(String projectId) {
        // 初始化版本V1.0, 初始化版本配置项
        ProjectVersion defaultVersion = new ProjectVersion();
        defaultVersion.setId(IDGenerator.nextStr());
        defaultVersion.setProjectId(projectId);
        defaultVersion.setName(DEFAULT_VERSION);
        defaultVersion.setStatus(DEFAULT_VERSION_STATUS);
        defaultVersion.setLatest(true);
        defaultVersion.setCreateTime(System.currentTimeMillis());
        defaultVersion.setCreateUser(InternalUserRole.ADMIN.getValue());
        projectVersionMapper.insert(defaultVersion);
        ProjectApplication projectApplication = new ProjectApplication();
        projectApplication.setProjectId(projectId);
        projectApplication.setType(ProjectApplicationType.VERSION.VERSION_ENABLE.name());
        projectApplication.setTypeValue("FALSE");
        projectApplicationService.update(projectApplication, "");
        LogUtils.info("初始化当前项目[" + projectId + "]相关版本资源");
    }
}
