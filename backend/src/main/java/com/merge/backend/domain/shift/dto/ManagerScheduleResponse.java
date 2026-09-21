package com.merge.backend.domain.shift.dto;

import com.merge.backend.domain.shift.entity.Schedule;
import com.merge.backend.domain.shift.entity.ScheduleStatus;
import com.merge.backend.domain.shift.entity.Shift;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ManagerScheduleResponse(
    Long scheduleId,
    Long workplaceId,
    LocalDate weekStartDate,
    ScheduleStatus status,
    LocalDateTime publishedAt,
    List<ScheduleShiftResponse> shifts
) {

    public static ManagerScheduleResponse from(
        Schedule schedule,
        List<Shift> shifts
    ) {

        List<ScheduleShiftResponse> shiftResponses =
            shifts.stream()
                .map(ScheduleShiftResponse::from)
                .toList();

        return new ManagerScheduleResponse(
            schedule.getId(),
            schedule.getWorkplace().getId(),
            schedule.getWeekStartDate(),
            schedule.getStatus(),
            schedule.getPublishedAt(),
            shiftResponses
        );
    }

}