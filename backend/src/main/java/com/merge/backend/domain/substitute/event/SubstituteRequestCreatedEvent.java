package com.merge.backend.domain.substitute.event;

import java.util.List;

public record SubstituteRequestCreatedEvent(
    Long requestId,
    List<Long> candidateMemberIds
) {
    public SubstituteRequestCreatedEvent {
        candidateMemberIds = List.copyOf(candidateMemberIds);
    }
}