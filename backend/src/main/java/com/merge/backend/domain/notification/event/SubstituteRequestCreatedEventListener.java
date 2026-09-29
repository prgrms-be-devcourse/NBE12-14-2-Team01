package com.merge.backend.domain.notification.event;

import com.merge.backend.domain.notification.service.NotificationService;
import com.merge.backend.domain.substitute.event.SubstituteRequestCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubstituteRequestCreatedEventListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleSubstituteRequestCreated(
        SubstituteRequestCreatedEvent event
    ) {
        try {
            notificationService.createSubstituteRequestReceivedNotifications(
                event.candidateMemberIds()
            );
        } catch (Exception e) {
            log.error(
                "대타 요청 도착 알림 생성 실패. "
                    + "requestId={}, candidateMemberIds={}",
                event.requestId(),
                event.candidateMemberIds(),
                e
            );
        }
    }
}