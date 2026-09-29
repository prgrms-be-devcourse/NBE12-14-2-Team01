package com.merge.backend.domain.substitute.event;

import java.util.List;

public record SubstituteCandidateAcceptedEvent(
    Long requestId,
    List<Long> managerMemberIds
) {
    public SubstituteCandidateAcceptedEvent {
        managerMemberIds = List.copyOf(managerMemberIds);
    }
}