package io.vanguard.testops.system.job;

import io.vanguard.testops.project.domain.Notification;
import io.vanguard.testops.project.domain.NotificationExample;
import io.vanguard.testops.project.mapper.NotificationMapper;
import io.vanguard.testops.system.notice.constants.NotificationConstants;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CleanNotificationJobTests {

    @Resource
    private CleanNotificationJob cleanNotificationJob;

    @Resource
    private NotificationMapper notificationMapper;

    void saveNotice() {
        Notification notification = new Notification();
        notification.setSubject("功能用例更新通知");
        notification.setOperator("admin");
        notification.setOperation("UPDATE");
        notification.setResourceType("FUNCTIONAL_CASE_TASK");
        notification.setOrganizationId("10001");
        notification.setProjectId("10000100001");
        notification.setResourceName("功能用例导入测4");
        notification.setType("SYSTEM_NOTICE");
        notification.setStatus(NotificationConstants.Status.UNREAD.name());
        notification.setCreateTime(1701659702000L);
        notification.setReceiver("admin");
        notification.setResourceId("cleanNotification");
        notification.setContent("nihao");
        notificationMapper.insert(notification);
    }

    @Test
    @Order(0)
    public void cleanupNotificationSuccess(){
        saveNotice();
        NotificationExample notificationExample = new NotificationExample();
        notificationExample.createCriteria().andResourceIdEqualTo("cleanNotification");
        List<Notification> notifications = notificationMapper.selectByExample(notificationExample);
        Assertions.assertTrue(CollectionUtils.isNotEmpty(notifications));
        cleanNotificationJob.cleanupNotification();
        notifications = notificationMapper.selectByExample(notificationExample);
        Assertions.assertTrue(CollectionUtils.isEmpty(notifications));
    }
}
