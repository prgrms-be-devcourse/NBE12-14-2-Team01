package com.merge.backend.domain.notification.dto;


import com.merge.backend.domain.notification.entity.Notification;
import java.time.LocalDateTime;

public record NotificationReadResponse(
    Long notificationId,
    LocalDateTime readAt
) {

    public static NotificationReadResponse from(
        Notification notification
    ) {
        return new NotificationReadResponse(
            notification.getId(),
            notification.getReadAt()
        );
    }
}