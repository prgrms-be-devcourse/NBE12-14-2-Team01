package com.merge.backend.domain.shift.event;

import java.util.List;

public record SchedulePublishedEvent(
    Long scheduleId,
    List<Long> recipientMemberIds
) {

    public SchedulePublishedEvent {
        recipientMemberIds =
            List.copyOf(recipientMemberIds);
    }
}
