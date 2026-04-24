package io.vanguard.testops.project.service;

import io.vanguard.testops.project.domain.MessageTask;
import io.vanguard.testops.project.domain.MessageTaskExample;
import io.vanguard.testops.project.domain.ProjectRobot;
import io.vanguard.testops.project.domain.ProjectRobotExample;
import io.vanguard.testops.project.dto.ProjectRobotDTO;
import io.vanguard.testops.project.mapper.MessageTaskMapper;
import io.vanguard.testops.project.mapper.ProjectRobotMapper;
import io.vanguard.testops.sdk.constants.HttpMethodConstants;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.system.dto.sdk.request.MessageTaskRequest;
import io.vanguard.testops.system.log.constants.OperationLogModule;
import io.vanguard.testops.system.log.constants.OperationLogType;
import io.vanguard.testops.system.log.dto.LogDTO;
import io.vanguard.testops.system.notice.constants.NoticeConstants;
import io.vanguard.testops.system.notice.support.template.MessageTemplateUtils;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional(rollbackFor = Exception.class)
public class MessageTaskLogService {

    @Resource
    private ProjectRobotMapper projectRobotMapper;

    @Resource
    private MessageTaskMapper messageTaskMapper;

    /**
     * 添加接口日志
     *
     * @param messageTaskRequest 消息配置参数
     * @return LogDTO
     */
    public LogDTO addLog(MessageTaskRequest messageTaskRequest) {
        MessageTaskExample messageTaskExample = new MessageTaskExample();
        Map<String, String> taskTypeMap = MessageTemplateUtils.getTaskTypeMap();
        //如果只选了用户，没有选机器人，默认机器人为站内信
        String robotId = setDefaultRobot(messageTaskRequest.getProjectId(), messageTaskRequest.getRobotId());
        messageTaskExample.createCriteria().andProjectIdEqualTo(messageTaskRequest.getProjectId())
                .andProjectRobotIdEqualTo(robotId).andTaskTypeEqualTo(messageTaskRequest.getTaskType()).andEventEqualTo(messageTaskRequest.getEvent());
        List<MessageTask> messageTasks = messageTaskMapper.selectByExample(messageTaskExample);
        LogDTO dto = new LogDTO(
                messageTaskRequest.getProjectId(),
                "",
                messageTaskRequest.getTaskType()+messageTaskRequest.getEvent(),
                null,
                OperationLogType.UPDATE.name(),
                OperationLogModule.PROJECT_MANAGEMENT_MESSAGE_MANAGEMENT_CONFIG,
                taskTypeMap.get(messageTaskRequest.getTaskType()));

        dto.setPath("/notice/message/task/save");
        dto.setMethod(HttpMethodConstants.POST.name());
        if (CollectionUtils.isNotEmpty(messageTasks)) {
            dto.setOriginalValue(JSON.toJSONBytes(messageTasks.getFirst()));
        }
        return dto;
    }



    public LogDTO addRobotLog(ProjectRobotDTO projectRobotDTO){
        LogDTO dto = new LogDTO(
                projectRobotDTO.getProjectId(),
                "",
                projectRobotDTO.getId(),
                null,
                OperationLogType.ADD.name(),
                OperationLogModule.PROJECT_MANAGEMENT_MESSAGE_MANAGEMENT_ROBOT,
                projectRobotDTO.getName());

        dto.setPath("/project/robot/add");
        dto.setMethod(HttpMethodConstants.POST.name());
        dto.setOriginalValue(JSON.toJSONBytes(projectRobotDTO));
        return dto;
    }

    public LogDTO updateRobotLog(ProjectRobotDTO projectRobotDTO){
        ProjectRobot projectRobot = projectRobotMapper.selectByPrimaryKey(projectRobotDTO.getId());
        if (projectRobot != null) {
            LogDTO dto = new LogDTO(
                    projectRobotDTO.getProjectId(),
                    "",
                    projectRobotDTO.getId(),
                    projectRobot.getCreateUser(),
                    OperationLogType.UPDATE.name(),
                    OperationLogModule.PROJECT_MANAGEMENT_MESSAGE_MANAGEMENT_ROBOT,
                    projectRobotDTO.getName()+"webhook:"+projectRobot.getWebhook()+ "——>" +"webhook:"+ projectRobotDTO.getWebhook() );

            dto.setPath("/project/robot/update");
            dto.setMethod(HttpMethodConstants.POST.name());
            dto.setOriginalValue(JSON.toJSONBytes(projectRobot));
            return dto;
        }
        return null;
    }


    public LogDTO delRobotLog(String id){
        ProjectRobot projectRobot = projectRobotMapper.selectByPrimaryKey(id);
        if (projectRobot != null) {
            LogDTO dto = new LogDTO(
                    projectRobot.getProjectId(),
                    "",
                    id,
                    projectRobot.getCreateUser(),
                    OperationLogType.DELETE.name(),
                    OperationLogModule.PROJECT_MANAGEMENT_MESSAGE_MANAGEMENT_ROBOT,
                    projectRobot.getName());

            dto.setPath("/project/robot/delete");
            dto.setMethod(HttpMethodConstants.GET.name());
            dto.setOriginalValue(JSON.toJSONBytes(projectRobot));
            return dto;
        }
        return null;
    }

    /**
     * 查询默认机器人id
     *
     * @param projectId 项目id
     * @param robotId   机器人id
     * @return String
     */
    private String setDefaultRobot(String projectId, String robotId) {
        if (StringUtils.isBlank(robotId)) {
            ProjectRobotExample projectRobotExample = new ProjectRobotExample();
            projectRobotExample.createCriteria().andProjectIdEqualTo(projectId).andPlatformEqualTo(NoticeConstants.Type.IN_SITE);
            List<ProjectRobot> projectRobots = projectRobotMapper.selectByExample(projectRobotExample);
            robotId = projectRobots.getFirst().getId();
        }
        return robotId;
    }
}
