package com.merge.backend.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.merge.backend.domain.notification.dto.NotificationReadResponse;
import com.merge.backend.domain.notification.entity.Notification;
import com.merge.backend.domain.notification.entity.NotificationType;
import com.merge.backend.domain.notification.exception.NotificationErrorCode;
import com.merge.backend.domain.notification.repository.NotificationRepository;
import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.global.config.TestClock;
import com.merge.backend.global.exception.BusinessException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationServiceTest {

    private NotificationRepository notificationRepository;
    private NotificationService notificationService;
    private TestClock testClock;

    @BeforeEach
    void setUp() {
        notificationRepository =
            mock(NotificationRepository.class);

        testClock = new TestClock(
            Instant.parse("2026-09-15T01:00:00Z"),
            ZoneId.of("Asia/Seoul")
        );

        notificationService = new NotificationService(
            notificationRepository,
            testClock
        );
    }

    @Test
    void createNotification() {
        WorkplaceMember recipientMember =
            mock(WorkplaceMember.class);

        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Notification notification =
            notificationService.create(
                recipientMember,
                NotificationType.SCHEDULE_PUBLISHED,
                "새로운 주간 근무표가 공개되었습니다."
            );

        assertThat(notification.getRecipientMember())
            .isSameAs(recipientMember);

        assertThat(notification.getType())
            .isEqualTo(
                NotificationType.SCHEDULE_PUBLISHED
            );

        assertThat(notification.getMessage())
            .isEqualTo(
                "새로운 주간 근무표가 공개되었습니다."
            );

        assertThat(notification.getReadAt()).isNull();

        verify(notificationRepository)
            .save(any(Notification.class));
    }

    @Test
    void markAsRead() {
        Long actorUserId = 1L;
        Long notificationId = 100L;

        User recipientUser =
            mock(User.class);

        WorkplaceMember recipientMember =
            mock(WorkplaceMember.class);

        when(recipientMember.getUser())
            .thenReturn(recipientUser);

        when(recipientUser.getId())
            .thenReturn(actorUserId);

        Notification notification = new Notification(
            recipientMember,
            NotificationType.SCHEDULE_PUBLISHED,
            "새로운 주간 근무표가 공개되었습니다."
        );

        when(notificationRepository.findById(notificationId))
            .thenReturn(Optional.of(notification));

        NotificationReadResponse result =
            notificationService.markAsRead(
                actorUserId,
                notificationId
            );

        LocalDateTime expectedReadAt =
            LocalDateTime.of(
                2026,
                9,
                15,
                10,
                0
            );

        assertThat(notification.getReadAt())
            .isEqualTo(expectedReadAt);

        assertThat(result.readAt())
            .isEqualTo(expectedReadAt);
    }

    @Test
    void markAsReadFailsWhenNotificationNotFound() {
        Long actorUserId = 1L;
        Long notificationId = 100L;

        when(notificationRepository.findById(notificationId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            notificationService.markAsRead(
                actorUserId,
                notificationId
            )
        )
            .isInstanceOf(BusinessException.class)
            .satisfies(exception -> {
                BusinessException businessException =
                    (BusinessException) exception;

                assertThat(businessException.getErrorCode())
                    .isEqualTo(
                        NotificationErrorCode.NOT_FOUND_NOTIFICATION
                    );
            });
    }

    @Test
    void markAsReadFailsWhenNotificationBelongsToAnotherUser() {
        Long actorUserId = 1L;
        Long recipientUserId = 2L;
        Long notificationId = 100L;

        User recipientUser =
            mock(User.class);

        WorkplaceMember recipientMember =
            mock(WorkplaceMember.class);

        when(recipientMember.getUser())
            .thenReturn(recipientUser);

        when(recipientUser.getId())
            .thenReturn(recipientUserId);

        Notification notification = new Notification(
            recipientMember,
            NotificationType.SCHEDULE_PUBLISHED,
            "새로운 주간 근무표가 공개되었습니다."
        );

        when(notificationRepository.findById(notificationId))
            .thenReturn(Optional.of(notification));

        assertThatThrownBy(() ->
            notificationService.markAsRead(
                actorUserId,
                notificationId
            )
        )
            .isInstanceOf(BusinessException.class)
            .satisfies(exception -> {
                BusinessException businessException =
                    (BusinessException) exception;

                assertThat(businessException.getErrorCode())
                    .isEqualTo(
                        NotificationErrorCode.FORBIDDEN_ACCESS
                    );
            });

        assertThat(notification.getReadAt()).isNull();
    }
}