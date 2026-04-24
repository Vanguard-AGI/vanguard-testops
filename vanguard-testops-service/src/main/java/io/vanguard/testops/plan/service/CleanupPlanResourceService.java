package io.vanguard.testops.plan.service;

import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.system.service.CleanupProjectResourceService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class CleanupPlanResourceService implements CleanupProjectResourceService {

    @Resource
    private TestPlanService testPlanService;
    @Override
    public void deleteResources(String projectId) {
        LogUtils.info("删除当前项目[" + projectId + "]的测试计划资源开始");
        testPlanService.deleteByProjectId(projectId);
        LogUtils.info("删除当前项目[" + projectId + "]的测试计划资源结束");
    }

}
