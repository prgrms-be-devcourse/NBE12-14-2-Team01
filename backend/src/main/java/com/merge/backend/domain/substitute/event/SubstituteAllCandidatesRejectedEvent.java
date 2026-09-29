package com.merge.backend.domain.substitute.event;

import java.util.List;

public record SubstituteAllCandidatesRejectedEvent(
    Long requestId,
    List<Long> recipientMemberIds
) {
    public SubstituteAllCandidatesRejectedEvent {
        recipientMemberIds = List.copyOf(recipientMemberIds);
    }
}