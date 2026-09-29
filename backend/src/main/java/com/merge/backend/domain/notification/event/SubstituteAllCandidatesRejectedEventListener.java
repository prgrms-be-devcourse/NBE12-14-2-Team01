package com.merge.backend.domain.notification.event;

import com.merge.backend.domain.notification.service.NotificationService;
import com.merge.backend.domain.substitute.event.SubstituteAllCandidatesRejectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubstituteAllCandidatesRejectedEventListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleSubstituteAllCandidatesRejected(
        SubstituteAllCandidatesRejectedEvent event
    ) {
        try {
            notificationService.createSubstituteAllRejectedNotifications(
                event.recipientMemberIds()
            );
        } catch (Exception e) {
            log.error(
                "대타 후보 전원 거절 알림 생성 실패. "
                    + "requestId={}, recipientMemberIds={}",
                event.requestId(),
                event.recipientMemberIds(),
                e
            );
        }
    }
}