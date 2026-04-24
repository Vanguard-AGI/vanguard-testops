package io.vanguard.testops.project.service;

import io.vanguard.testops.project.domain.ProjectApplicationExample;
import io.vanguard.testops.project.mapper.ProjectApplicationMapper;
import io.vanguard.testops.system.runtime.schedule.ScheduleService;
import io.vanguard.testops.system.service.CleanupProjectResourceService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * @author Jan
 */
@Component
public class CleanupApplicationResourceService implements CleanupProjectResourceService {

    @Resource
    private ScheduleService scheduleService;
    @Resource
    private ProjectApplicationMapper projectApplicationMapper;

    @Override
    public void deleteResources(String projectId) {
        scheduleService.deleteByProjectId(projectId);
        ProjectApplicationExample example = new ProjectApplicationExample();
        example.createCriteria().andProjectIdEqualTo(projectId);
        projectApplicationMapper.deleteByExample(example);
    }

}
