package io.vanguard.testops.bug.service;

import io.vanguard.testops.bug.domain.Bug;
import io.vanguard.testops.bug.domain.BugExample;
import io.vanguard.testops.bug.mapper.BugMapper;
import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.system.service.CleanupProjectResourceService;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 缺陷相关资源清理
 */
@Component
public class CleanupBugResourceServiceImpl implements CleanupProjectResourceService {

    @Resource
    private BugMapper bugMapper;
    @Resource
    private BugCommonService bugCommonService;

    /**
     * 清理当前项目相关缺陷资源
     * @param projectId 项目ID
     */
    @Async
    @Override
    public void deleteResources(String projectId) {
        LogUtils.info("删除当前项目[" + projectId + "]相关缺陷测试资源");
        List<String> deleteIds = getBugIds(projectId);
        if (CollectionUtils.isNotEmpty(deleteIds)) {
            // 清理缺陷
            deleteBug(deleteIds);
            // 清空关联关系
            bugCommonService.clearAssociateResource(projectId, deleteIds);
        }
    }

    /**
     * 获取当前项目下所有缺陷ID集合
     * @param projectId 项目ID
     * @return 缺陷ID集合
     */
    private List<String> getBugIds(String projectId) {
        BugExample example = new BugExample();
        example.createCriteria().andProjectIdEqualTo(projectId);
        List<Bug> bugs = bugMapper.selectByExample(example);
        return bugs.stream().map(Bug::getId).toList();
    }

    /**
     * 清理缺陷
     * @param bugIds 缺陷ID集合
     */
    private void deleteBug(List<String> bugIds) {
        BugExample example = new BugExample();
        example.createCriteria().andIdIn(bugIds);
        bugMapper.deleteByExample(example);
    }
}
