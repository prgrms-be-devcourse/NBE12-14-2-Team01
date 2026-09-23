package com.merge.backend.domain.notification.dto;

import com.merge.backend.domain.notification.entity.Notification;
import com.merge.backend.domain.notification.entity.NotificationType;
import java.time.LocalDateTime;

public record NotificationResponse(
    Long notificationId,
    Long workplaceId,
    String workplaceName,
    NotificationType type,
    String message,
    LocalDateTime createdAt,
    LocalDateTime readAt
) {

    public static NotificationResponse from(
        Notification notification
    ) {
        return new NotificationResponse(
            notification.getId(),
            notification.getRecipientMember()
                .getWorkplace()
                .getId(),
            notification.getRecipientMember()
                .getWorkplace()
                .getName(),
            notification.getType(),
            notification.getMessage(),
            notification.getCreateDate(),
            notification.getReadAt()
        );
    }
}
