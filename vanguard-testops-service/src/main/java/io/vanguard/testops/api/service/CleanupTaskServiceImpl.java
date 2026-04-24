package io.vanguard.testops.api.service;

import io.vanguard.testops.sdk.constants.ProjectApplicationType;
import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.system.domain.ExecTask;
import io.vanguard.testops.system.domain.ExecTaskExample;
import io.vanguard.testops.system.domain.ExecTaskItem;
import io.vanguard.testops.system.domain.ExecTaskItemExample;
import io.vanguard.testops.system.mapper.ExecTaskItemMapper;
import io.vanguard.testops.system.mapper.ExecTaskMapper;
import io.vanguard.testops.system.mapper.ExtExecTaskMapper;
import io.vanguard.testops.system.service.BaseCleanUpReport;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.vanguard.testops.sdk.util.ShareUtil.getCleanDate;

/**
 * @author Jan
 */
@Component
@Transactional(rollbackFor = Exception.class)
public class CleanupTaskServiceImpl implements BaseCleanUpReport {

    @Resource
    private ExtExecTaskMapper extExecTaskMapper;
    @Resource
    private ExecTaskMapper execTaskMapper;
    @Resource
    private ExecTaskItemMapper execTaskItemMapper;


    @Override
    public void cleanReport(Map<String, String> map, String projectId) {
        LogUtils.info("清理当前项目[" + projectId + "]即时任务");
        String expr = map.get(ProjectApplicationType.TASK.TASK_RECORD.name());
        long timeMills = getCleanDate(expr);
        List<String> cleanTaskIds = extExecTaskMapper.getTaskIdsByTime(timeMills, projectId);
        List<ExecTaskItem> execTaskItems = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(cleanTaskIds)) {
            ExecTaskItemExample itemExample = new ExecTaskItemExample();
            itemExample.createCriteria().andTaskIdIn(cleanTaskIds);
            execTaskItems = execTaskItemMapper.selectByExample(itemExample);
        }
        List<String> cleanTaskItemIds = execTaskItems.stream().map(ExecTaskItem::getId).toList();
        LogUtils.info("清理当前项目[" + projectId + "]即时任务, 共[" + (cleanTaskIds.size() + cleanTaskItemIds.size()) + "]条");
        if (CollectionUtils.isNotEmpty(cleanTaskIds)) {
            ExecTaskExample example = new ExecTaskExample();
            example.createCriteria().andIdIn(cleanTaskIds);
            ExecTask execTask = new ExecTask();
            execTask.setDeleted(true);
            execTaskMapper.updateByExampleSelective(execTask, example);
        }
        if (CollectionUtils.isNotEmpty(cleanTaskItemIds)) {
            ExecTaskItemExample example = new ExecTaskItemExample();
            example.createCriteria().andIdIn(cleanTaskItemIds);
            ExecTaskItem execTaskItem = new ExecTaskItem();
            execTaskItem.setDeleted(true);
            execTaskItemMapper.updateByExampleSelective(execTaskItem, example);
        }
        LogUtils.info("清理当前项目[" + projectId + "]即时任务结束!");
    }
}
