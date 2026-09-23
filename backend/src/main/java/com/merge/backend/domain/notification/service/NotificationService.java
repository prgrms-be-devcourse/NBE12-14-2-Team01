package com.merge.backend.domain.notification.service;

import com.merge.backend.domain.notification.dto.NotificationReadResponse;
import com.merge.backend.domain.notification.entity.Notification;
import com.merge.backend.domain.notification.entity.NotificationType;
import com.merge.backend.domain.notification.exception.NotificationErrorCode;
import com.merge.backend.domain.notification.repository.NotificationRepository;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.global.exception.BusinessException;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final Clock clock;

    public Notification create(
        WorkplaceMember recipientMember,
        NotificationType type,
        String message
    ) {
        Notification notification = new Notification(
            recipientMember,
            type,
            message
        );

        return notificationRepository.save(notification);
    }

    @Transactional
    public NotificationReadResponse markAsRead(
        Long actorUserId,
        Long notificationId
    ) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() ->
                new BusinessException(
                    NotificationErrorCode.NOT_FOUND_NOTIFICATION
                )
            );

        Long recipientUserId = notification
            .getRecipientMember()
            .getUser()
            .getId();

        if (!recipientUserId.equals(actorUserId)) {
            throw new BusinessException(
                NotificationErrorCode.FORBIDDEN_ACCESS
            );
        }

        notification.markAsRead(LocalDateTime.now(clock));

        return NotificationReadResponse.from(
            notification
        );
    }

}
