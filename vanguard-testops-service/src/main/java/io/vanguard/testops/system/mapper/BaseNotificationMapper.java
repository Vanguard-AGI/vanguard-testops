package io.vanguard.testops.system.mapper;


import io.vanguard.testops.system.dto.sdk.request.NotificationRequest;
import io.vanguard.testops.system.log.dto.NotificationDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BaseNotificationMapper {

    List<NotificationDTO> listNotification(@Param("request") NotificationRequest notificationRequest);

    void deleteByTime(@Param("timestamp") long timestamp);

}
