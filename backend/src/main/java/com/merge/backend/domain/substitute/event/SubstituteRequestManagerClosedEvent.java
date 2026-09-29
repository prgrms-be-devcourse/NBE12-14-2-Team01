package com.merge.backend.domain.substitute.event;

import java.util.List;

public record SubstituteRequestManagerClosedEvent(
    Long requestId,
    List<Long> recipientMemberIds
) {
    public SubstituteRequestManagerClosedEvent {
        recipientMemberIds = List.copyOf(recipientMemberIds);
    }
}