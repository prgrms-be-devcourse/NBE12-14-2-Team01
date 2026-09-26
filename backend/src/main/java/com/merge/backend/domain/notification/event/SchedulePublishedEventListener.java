package com.merge.backend.domain.notification.event;

import com.merge.backend.domain.notification.service.NotificationService;
import com.merge.backend.domain.shift.event.SchedulePublishedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulePublishedEventListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleSchedulePublished(
        SchedulePublishedEvent event
    ) {
        try {
            notificationService.createSchedulePublishedNotifications(
                event.recipientMemberIds()
            );
        } catch (Exception e) {
            log.error(
                "Schedule 공개 알림 생성 실패. "
                    + "scheduleId={}, recipientMemberIds={}",
                event.scheduleId(),
                event.recipientMemberIds(),
                e
            );
        }
    }

}