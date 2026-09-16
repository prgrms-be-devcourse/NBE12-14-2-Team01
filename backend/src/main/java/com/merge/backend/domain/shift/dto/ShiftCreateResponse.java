package com.merge.backend.domain.shift.dto;

import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.entity.ShiftStatus;
import java.time.LocalDateTime;

public record ShiftCreateResponse(

    Long scheduleId,
    Long memberId,
    String memberName,
    LocalDateTime startAt,
    LocalDateTime endAt,
    ShiftStatus status

) {
    public static ShiftCreateResponse from(Shift shift){
        return new ShiftCreateResponse(
            shift.getSchedule().getId(),
            shift.getMember().getId(),
            shift.getMember().getUser().getName(),
            shift.getStartAt(),
            shift.getEndAt(),
            shift.getStatus()
        );
    }
}
