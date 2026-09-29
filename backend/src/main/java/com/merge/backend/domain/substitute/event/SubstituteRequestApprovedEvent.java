package com.merge.backend.domain.substitute.event;

public record SubstituteRequestApprovedEvent(
    Long requestId,
    Long requesterMemberId,
    Long acceptedMemberId
) {
}