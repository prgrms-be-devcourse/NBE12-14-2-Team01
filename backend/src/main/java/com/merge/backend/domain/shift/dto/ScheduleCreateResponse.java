package com.merge.backend.domain.shift.dto;

import com.merge.backend.domain.shift.entity.Schedule;
import com.merge.backend.domain.shift.entity.ScheduleStatus;
import java.time.LocalDate;

public record ScheduleCreateResponse(
    Long scheduleId,
    Long workplaceId,
    LocalDate weekStartDate,
    ScheduleStatus status,
    int shiftCount
) {

    public static ScheduleCreateResponse from(
        Schedule schedule,
        int shiftCount
    ) {
        return new ScheduleCreateResponse(
            schedule.getId(),
            schedule.getWorkplace().getId(),
            schedule.getWeekStartDate(),
            schedule.getStatus(),
            shiftCount
        );
    }
}