package com.merge.backend.domain.notification.event;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.merge.backend.domain.notification.service.NotificationService;
import com.merge.backend.domain.shift.event.SchedulePublishedEvent;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SchedulePublishedEventListenerIntegrationTest {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    @DisplayName(
        "SchedulePublishedEvent는 원 Transaction commit 후 처리된다"
    )
    void handlesSchedulePublishedEventAfterCommit() {

        // given
        List<Long> recipientMemberIds =
            List.of(
                100L,
                200L
            );

        SchedulePublishedEvent event =
            new SchedulePublishedEvent(
                20L,
                recipientMemberIds
            );

        // when - 아직 Transaction 안
        applicationEventPublisher.publishEvent(event);

        // then - commit 전에는 Listener가 실행되지 않아야 한다.
        verifyNoInteractions(notificationService);

        // when - 실제 test Transaction을 commit한다.
        TestTransaction.flagForCommit();
        TestTransaction.end();

        // then - commit 후 Listener가 실행된다.
        verify(notificationService)
            .createSchedulePublishedNotifications(
                recipientMemberIds
            );
    }

}
