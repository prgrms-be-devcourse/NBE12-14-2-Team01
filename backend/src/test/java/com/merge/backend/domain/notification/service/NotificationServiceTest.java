package com.merge.backend.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.merge.backend.domain.notification.dto.NotificationReadResponse;
import com.merge.backend.domain.notification.dto.NotificationResponse;
import com.merge.backend.domain.notification.entity.Notification;
import com.merge.backend.domain.notification.entity.NotificationType;
import com.merge.backend.domain.notification.exception.NotificationErrorCode;
import com.merge.backend.domain.notification.repository.NotificationRepository;
import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.workplace.entity.Workplace;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.repository.WorkplaceMemberRepository;
import com.merge.backend.global.config.TestClock;
import com.merge.backend.global.exception.BusinessException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificationServiceTest {

    private NotificationRepository notificationRepository;
    private WorkplaceMemberRepository workplaceMemberRepository;
    private NotificationService notificationService;
    private TestClock testClock;

    @BeforeEach
    void setUp() {
        notificationRepository =
            mock(NotificationRepository.class);

        workplaceMemberRepository =
            mock(WorkplaceMemberRepository.class);

        testClock = new TestClock(
            Instant.parse("2026-09-15T01:00:00Z"),
            ZoneId.of("Asia/Seoul")
        );

        notificationService = new NotificationService(
            notificationRepository,
            workplaceMemberRepository,
            testClock
        );
    }

    @Test
    @DisplayName("알림을 생성한다")
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
    @DisplayName("알림을 읽음 처리한다")
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
    @DisplayName("존재하지 않는 알림은 읽음 처리할 수 없다")
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
    @DisplayName("다른 사용자의 알림은 읽음 처리할 수 없다")
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

    @Test
    @DisplayName("내 알림 목록을 조회한다")
    void getNotifications() {
        Long actorUserId = 1L;

        Notification notification =
            mock(Notification.class);

        WorkplaceMember recipientMember =
            mock(WorkplaceMember.class);

        Workplace workplace =
            mock(Workplace.class);

        LocalDateTime createdAt =
            LocalDateTime.of(
                2026,
                9,
                15,
                10,
                0
            );

        when(notification.getId())
            .thenReturn(100L);

        when(notification.getRecipientMember())
            .thenReturn(recipientMember);

        when(recipientMember.getWorkplace())
            .thenReturn(workplace);

        when(workplace.getId())
            .thenReturn(10L);

        when(workplace.getName())
            .thenReturn("SWITCH 카페");

        when(notification.getType())
            .thenReturn(
                NotificationType.SCHEDULE_PUBLISHED
            );

        when(notification.getMessage())
            .thenReturn(
                "새로운 주간 근무표가 공개되었습니다."
            );

        when(notification.getCreateDate())
            .thenReturn(createdAt);

        when(notification.getReadAt())
            .thenReturn(null);

        when(notificationRepository.findAllByUserId(actorUserId))
            .thenReturn(List.of(notification));

        List<NotificationResponse> result =
            notificationService.getNotifications(
                actorUserId
            );

        assertThat(result).hasSize(1);

        NotificationResponse response =
            result.get(0);

        assertThat(response.notificationId())
            .isEqualTo(100L);

        assertThat(response.workplaceId())
            .isEqualTo(10L);

        assertThat(response.workplaceName())
            .isEqualTo("SWITCH 카페");

        assertThat(response.type())
            .isEqualTo(
                NotificationType.SCHEDULE_PUBLISHED
            );

        assertThat(response.message())
            .isEqualTo(
                "새로운 주간 근무표가 공개되었습니다."
            );

        assertThat(response.createdAt())
            .isEqualTo(createdAt);

        assertThat(response.readAt())
            .isNull();

        verify(notificationRepository)
            .findAllByUserId(actorUserId);
    }

    @Test
    @DisplayName("읽은 알림과 읽지 않은 알림을 모두 조회한다")
    void getNotificationsIncludesReadAndUnreadNotifications() {
        Long actorUserId = 1L;

        Notification unreadNotification =
            mock(Notification.class);

        Notification readNotification =
            mock(Notification.class);

        WorkplaceMember unreadRecipientMember =
            mock(WorkplaceMember.class);

        WorkplaceMember readRecipientMember =
            mock(WorkplaceMember.class);

        Workplace workplace =
            mock(Workplace.class);

        LocalDateTime unreadCreatedAt =
            LocalDateTime.of(
                2026,
                9,
                15,
                11,
                0
            );

        LocalDateTime readCreatedAt =
            LocalDateTime.of(
                2026,
                9,
                15,
                10,
                0
            );

        LocalDateTime readAt =
            LocalDateTime.of(
                2026,
                9,
                15,
                10,
                30
            );

        when(unreadNotification.getId())
            .thenReturn(101L);

        when(unreadNotification.getRecipientMember())
            .thenReturn(unreadRecipientMember);

        when(unreadRecipientMember.getWorkplace())
            .thenReturn(workplace);

        when(unreadNotification.getType())
            .thenReturn(
                NotificationType.SCHEDULE_PUBLISHED
            );

        when(unreadNotification.getMessage())
            .thenReturn("새로운 주간 근무표가 공개되었습니다.");

        when(unreadNotification.getCreateDate())
            .thenReturn(unreadCreatedAt);

        when(unreadNotification.getReadAt())
            .thenReturn(null);

        when(readNotification.getId())
            .thenReturn(100L);

        when(readNotification.getRecipientMember())
            .thenReturn(readRecipientMember);

        when(readRecipientMember.getWorkplace())
            .thenReturn(workplace);

        when(readNotification.getType())
            .thenReturn(
                NotificationType.SUBSTITUTE_APPROVED
            );

        when(readNotification.getMessage())
            .thenReturn("대타 요청이 최종 승인되었습니다.");

        when(readNotification.getCreateDate())
            .thenReturn(readCreatedAt);

        when(readNotification.getReadAt())
            .thenReturn(readAt);

        when(workplace.getId())
            .thenReturn(10L);

        when(workplace.getName())
            .thenReturn("SWITCH 카페");

        when(notificationRepository.findAllByUserId(actorUserId))
            .thenReturn(
                List.of(
                    unreadNotification,
                    readNotification
                )
            );

        List<NotificationResponse> result =
            notificationService.getNotifications(
                actorUserId
            );

        assertThat(result).hasSize(2);

        assertThat(result.get(0).notificationId())
            .isEqualTo(101L);

        assertThat(result.get(0).readAt())
            .isNull();

        assertThat(result.get(1).notificationId())
            .isEqualTo(100L);

        assertThat(result.get(1).readAt())
            .isEqualTo(readAt);
    }

    @Test
    void createSchedulePublishedNotifications() {

        // given
        List<Long> recipientMemberIds =
            List.of(
                100L,
                200L
            );

        WorkplaceMember firstMember =
            mock(WorkplaceMember.class);

        WorkplaceMember secondMember =
            mock(WorkplaceMember.class);

        when(
            workplaceMemberRepository.findAllById(
                recipientMemberIds
            )
        ).thenReturn(
            List.of(
                firstMember,
                secondMember
            )
        );

        when(
            notificationRepository.save(
                any(Notification.class)
            )
        ).thenAnswer(
            invocation ->
                invocation.getArgument(0)
        );

        // when
        notificationService.createSchedulePublishedNotifications(
            recipientMemberIds
        );

        // then
        verify(workplaceMemberRepository)
            .findAllById(
                recipientMemberIds
            );

        ArgumentCaptor<Notification> notificationCaptor =
            ArgumentCaptor.forClass(
                Notification.class
            );

        verify(notificationRepository, times(2))
            .save(
                notificationCaptor.capture()
            );

        List<Notification> savedNotifications =
            notificationCaptor.getAllValues();

        assertThat(savedNotifications)
            .extracting(Notification::getRecipientMember)
            .containsExactlyInAnyOrder(
                firstMember,
                secondMember
            );

        assertThat(savedNotifications)
            .extracting(Notification::getType)
            .containsOnly(
                NotificationType.SCHEDULE_PUBLISHED
            );

        assertThat(savedNotifications)
            .extracting(Notification::getMessage)
            .containsOnly(
                "새로운 주간 근무표가 공개되었습니다."
            );
    }

}