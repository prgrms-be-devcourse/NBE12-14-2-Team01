package com.merge.backend.domain.substitute.event;

import java.util.List;

public record SubstituteNoCandidateEvent(
    Long shiftId,
    List<Long> managerMemberIds
) {
    public SubstituteNoCandidateEvent {
        managerMemberIds = List.copyOf(managerMemberIds);
    }
}