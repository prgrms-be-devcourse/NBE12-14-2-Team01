package com.merge.backend.domain.substitute.dto;

import com.merge.backend.domain.substitute.entity.RequestCloseReason;
import com.merge.backend.domain.substitute.entity.RequestStatus;
import com.merge.backend.domain.substitute.entity.SubstituteRequest;
import java.time.LocalDateTime;

public record SubstituteRequestCloseResponse(
    Long requestId,
    Long shiftId,
    RequestStatus status,
    RequestCloseReason closeReason,
    LocalDateTime closedAt
) {
    public static SubstituteRequestCloseResponse from(SubstituteRequest request) {
        return new SubstituteRequestCloseResponse(
            request.getId(),
            request.getShift().getId(),
            request.getStatus(),
            request.getCloseReason(),
            request.getClosedAt()
        );
    }
}
