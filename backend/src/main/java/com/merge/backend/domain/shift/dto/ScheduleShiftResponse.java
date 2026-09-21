package com.merge.backend.domain.shift.dto;

import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.entity.ShiftStatus;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;
import java.time.LocalDateTime;

public record ScheduleShiftResponse(
    Long shiftId,
    Long memberId,
    String memberName,
    WorkplaceRole role,
    LocalDateTime startAt,
    LocalDateTime endAt,
    ShiftStatus status
) {
    public static ScheduleShiftResponse from(
        Shift shift
    ) {
        return new ScheduleShiftResponse(
            shift.getId(),
            shift.getMember().getId(),
            shift.getMember().getUser().getName(),
            shift.getMember().getRole(),
            shift.getStartAt(),
            shift.getEndAt(),
            shift.getStatus()
        );
    }
}
