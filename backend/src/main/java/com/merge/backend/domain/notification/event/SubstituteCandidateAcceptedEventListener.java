package com.merge.backend.domain.notification.event;

import com.merge.backend.domain.notification.service.NotificationService;
import com.merge.backend.domain.substitute.event.SubstituteCandidateAcceptedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubstituteCandidateAcceptedEventListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleSubstituteCandidateAccepted(
        SubstituteCandidateAcceptedEvent event
    ) {
        try {
            notificationService.createSubstituteAcceptedNotifications(
                event.managerMemberIds()
            );
        } catch (Exception e) {
            log.error(
                "대타 수락 알림 생성 실패. "
                    + "requestId={}, managerMemberIds={}",
                event.requestId(),
                event.managerMemberIds(),
                e
            );
        }
    }
}