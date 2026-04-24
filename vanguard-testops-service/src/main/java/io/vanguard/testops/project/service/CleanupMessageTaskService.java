package io.vanguard.testops.project.service;

import io.vanguard.testops.project.domain.MessageTask;
import io.vanguard.testops.project.domain.MessageTaskBlobExample;
import io.vanguard.testops.project.domain.MessageTaskExample;
import io.vanguard.testops.project.mapper.MessageTaskBlobMapper;
import io.vanguard.testops.project.mapper.MessageTaskMapper;
import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.system.service.CleanupProjectResourceService;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CleanupMessageTaskService implements CleanupProjectResourceService {

    @Resource
    private MessageTaskMapper messageTaskMapper;
    @Resource
    private MessageTaskBlobMapper messageTaskBlobMapper;

    @Override
    public void deleteResources(String projectId) {
        MessageTaskExample messageTaskExample = new MessageTaskExample();
        messageTaskExample.createCriteria().andProjectIdEqualTo(projectId);
        List<MessageTask> messageTasks = messageTaskMapper.selectByExample(messageTaskExample);
        List<String> ids = messageTasks.stream().map(MessageTask::getId).toList();
        if (CollectionUtils.isNotEmpty(ids)) {
            MessageTaskBlobExample messageTaskBlobExample = new MessageTaskBlobExample();
            messageTaskBlobExample.createCriteria().andIdIn(ids);
            messageTaskBlobMapper.deleteByExample(messageTaskBlobExample);
        }
        messageTaskMapper.deleteByExample(messageTaskExample);
        LogUtils.info("删除当前项目[" + projectId + "]相关消息管理资源");
    }

}
