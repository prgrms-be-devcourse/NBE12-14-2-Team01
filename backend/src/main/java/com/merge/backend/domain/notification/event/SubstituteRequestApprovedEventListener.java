package com.merge.backend.domain.notification.event;

import com.merge.backend.domain.notification.service.NotificationService;
import com.merge.backend.domain.substitute.event.SubstituteRequestApprovedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubstituteRequestApprovedEventListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleSubstituteRequestApproved(
        SubstituteRequestApprovedEvent event
    ) {
        try {
            notificationService.createSubstituteApprovedNotifications(
                event.requesterMemberId(),
                event.acceptedMemberId()
            );
        } catch (Exception e) {
            log.error(
                "대체 근무 최종 승인 알림 생성 실패. "
                    + "requestId={}, requesterMemberId={}, "
                    + "acceptedMemberId={}",
                event.requestId(),
                event.requesterMemberId(),
                event.acceptedMemberId(),
                e
            );
        }
    }
}