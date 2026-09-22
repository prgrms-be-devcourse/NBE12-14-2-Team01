package com.merge.backend.domain.substitute.dto;

import com.merge.backend.domain.substitute.entity.RequestStatus;
import com.merge.backend.domain.substitute.entity.SubstituteRequest;
import java.time.LocalDateTime;

public record SubstituteRequestResponse(
    Long requestId,
    Long shiftId,
    RequestStatus status,
    Long previousMemberId,
    Long newMemberId,
    String newMemberName,
    LocalDateTime approvedAt,
    LocalDateTime closedAt
) {
    public static SubstituteRequestResponse from(SubstituteRequest request){
        return new SubstituteRequestResponse(
            request.getId(),
            request.getShift().getId(),
            RequestStatus.APPROVED,
            request.getRequesterMember().getId(),
            request.getShift().getMember().getId(),
            request.getShift().getMember().getUser().getName(),
            request.getApprovedAt(),
            request.getClosedAt()
        );
    }
}
