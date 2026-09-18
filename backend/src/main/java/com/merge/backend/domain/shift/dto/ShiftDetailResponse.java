package com.merge.backend.domain.shift.dto;

import com.merge.backend.domain.shift.entity.ScheduleStatus;
import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.entity.ShiftStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ShiftDetailResponse(

    Long shiftId,
    Long scheduleId,
    Long workplaceId,
    String workplaceName,
    LocalDate weekStartDate,
    LocalDateTime startAt,
    LocalDateTime endAt,
    ShiftStatus status,
    LocalDateTime publishedAt

) {
    public static ShiftDetailResponse from(Shift shift){
        return new ShiftDetailResponse(
            shift.getId(),
            shift.getSchedule().getId(),
            shift.getSchedule().getWorkplace().getId(),
            shift.getSchedule().getWorkplace().getName(),
            shift.getSchedule().getWeekStartDate(),
            shift.getStartAt(),
            shift.getEndAt(),
            shift.getStatus(),
            shift.getSchedule().getPublishedAt()
        );
    }
}
