package com.merge.backend.domain.notification.event;

import com.merge.backend.domain.notification.service.NotificationService;
import com.merge.backend.domain.substitute.event.SubstituteNoCandidateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubstituteNoCandidateEventListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleSubstituteNoCandidate(
        SubstituteNoCandidateEvent event
    ) {
        try {
            notificationService.createSubstituteNoCandidateNotifications(
                event.managerMemberIds()
            );
        } catch (Exception e) {
            log.error(
                "대타 후보 없음 알림 생성 실패. "
                    + "shiftId={}, managerMemberIds={}",
                event.shiftId(),
                event.managerMemberIds(),
                e
            );
        }
    }
}