package io.vanguard.testops.project.service;

import io.vanguard.testops.project.domain.ProjectVersionExample;
import io.vanguard.testops.project.mapper.ProjectApplicationMapper;
import io.vanguard.testops.project.mapper.ProjectVersionMapper;
import io.vanguard.testops.sdk.constants.ProjectApplicationType;
import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.system.service.CleanupProjectResourceService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 项目删除-清空版本资源
 *
 * @author Jan
 */
@Component
public class CleanupVersionResourceService implements CleanupProjectResourceService {

    @Resource
    private ProjectVersionMapper projectVersionMapper;
    @Resource
    private ProjectApplicationMapper projectApplicationMapper;

    @Override
    public void deleteResources(String projectId) {
        // 删除所有项目版本
        ProjectVersionExample example = new ProjectVersionExample();
        example.createCriteria().andProjectIdEqualTo(projectId);
        projectVersionMapper.deleteByExample(example);
        // 删除项目版本配置项
        projectApplicationMapper.deleteByPrimaryKey(projectId, ProjectApplicationType.VERSION.VERSION_ENABLE.name());
        LogUtils.info("清理当前项目[" + projectId + "]相关版本资源");
    }

}
