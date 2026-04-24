package io.vanguard.testops.project.service;

import io.vanguard.testops.project.domain.ProjectRobotExample;
import io.vanguard.testops.project.mapper.ProjectRobotMapper;
import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.system.service.CleanupProjectResourceService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class CleanupRobotResourceService implements CleanupProjectResourceService {

    @Resource
    private ProjectRobotMapper robotMapper;

    @Override
    public void deleteResources(String projectId) {
        ProjectRobotExample projectExample = new ProjectRobotExample();
        projectExample.createCriteria().andProjectIdEqualTo(projectId);
        robotMapper.deleteByExample(projectExample);
        LogUtils.info("删除当前项目[" + projectId + "]相关消息机器人资源");
    }
}
