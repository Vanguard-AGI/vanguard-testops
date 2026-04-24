package io.vanguard.testops.system.notice.sender.impl;


import io.vanguard.testops.project.domain.Notification;
import io.vanguard.testops.project.domain.Project;
import io.vanguard.testops.project.mapper.ProjectMapper;
import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.system.notice.MessageDetail;
import io.vanguard.testops.system.notice.NoticeModel;
import io.vanguard.testops.system.notice.Receiver;
import io.vanguard.testops.system.notice.constants.NotificationConstants;
import io.vanguard.testops.system.notice.sender.AbstractNoticeSender;
import io.vanguard.testops.system.service.NotificationService;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class InSiteNoticeSender extends AbstractNoticeSender {

    @Resource
    private NotificationService notificationService;
    @Resource
    private ProjectMapper projectMapper;

    public void sendAnnouncement(MessageDetail messageDetail, NoticeModel noticeModel, String context, String subjectText) {
        List<Receiver> receivers = super.getReceivers(noticeModel.getReceivers(), noticeModel.isExcludeSelf(), noticeModel.getOperator());
        if (CollectionUtils.isEmpty(receivers)) {
            return;
        }
        Project project = projectMapper.selectByPrimaryKey(messageDetail.getProjectId());
        LogUtils.info("发送站内通知: {}", receivers);
        receivers.forEach(receiver -> {
            Map<String, Object> paramMap = noticeModel.getParamMap();
            Notification notification = new Notification();
            notification.setSubject(subjectText);
            notification.setProjectId(messageDetail.getProjectId());
            notification.setOrganizationId(project.getOrganizationId());
            notification.setOperator(noticeModel.getOperator());
            notification.setOperation(noticeModel.getEvent());
            notification.setResourceId((String) paramMap.get("id"));
            notification.setResourceType(messageDetail.getTaskType());
            if (paramMap.get("name") != null) {
                notification.setResourceName((String) paramMap.get("name"));
            }
            if (paramMap.get("title") != null) {
                notification.setResourceName((String) paramMap.get("title"));
            }
            notification.setType(receiver.getType());
            notification.setStatus(NotificationConstants.Status.UNREAD.name());
            notification.setCreateTime(System.currentTimeMillis());
            notification.setReceiver(receiver.getUserId());
            notification.setContent(context);
            notificationService.sendAnnouncement(notification);
        });
    }

    @Override
    public void send(MessageDetail messageDetail, NoticeModel noticeModel) {
        String context = super.getContext(messageDetail, noticeModel);
        String subjectText = super.getSubjectText(messageDetail, noticeModel);
        sendAnnouncement(messageDetail, noticeModel, context, subjectText);
    }

}
