package com.merge.backend.domain.substitute.dto;


import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.substitute.entity.RequestCloseReason;
import com.merge.backend.domain.substitute.entity.RequestStatus;
import com.merge.backend.domain.substitute.entity.SubstituteRequest;
import com.merge.backend.domain.workplace.entity.Workplace;
import java.time.LocalDateTime;

public record SentSubstituteRequestResponse(

    Long requestId,
    Long shiftId,
    Long workplaceId,
    String workplaceName,
    LocalDateTime startAt,
    LocalDateTime endAt,
    RequestStatus status,
    RequestCloseReason closeReason,
    LocalDateTime createdAt,
    LocalDateTime approvedAt,
    LocalDateTime closedAt

) {

    public static SentSubstituteRequestResponse
            from(SubstituteRequest request, LocalDateTime now) {
        Shift shift = request.getShift();
        Workplace workplace = shift.getSchedule().getWorkplace();

        boolean isExpired =
            (request.getStatus() == RequestStatus.OPEN
                || request.getStatus() == RequestStatus.ACCEPTED)
                && !now.isBefore(shift.getStartAt());

        if (isExpired) {
            return new SentSubstituteRequestResponse(
                request.getId(),
                shift.getId(),
                workplace.getId(),
                workplace.getName(),
                shift.getStartAt(),
                shift.getEndAt(),
                RequestStatus.CLOSED,
                RequestCloseReason.EXPIRED,
                request.getCreateDate(),
                request.getApprovedAt(),
                now
            );
        }

        return new SentSubstituteRequestResponse(
            request.getId(),
            shift.getId(),
            workplace.getId(),
            workplace.getName(),
            shift.getStartAt(),
            shift.getEndAt(),
            request.getStatus(),
            request.getCloseReason(),
            request.getCreateDate(),
            request.getApprovedAt(),
            request.getClosedAt()
        );
    }
}
