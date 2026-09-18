package com.merge.backend.domain.shift.dto;

import com.merge.backend.domain.shift.entity.Schedule;
import com.merge.backend.domain.shift.entity.ScheduleStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SchedulePublishResponse(
    Long scheduleId,
    Long workplaceId,
    LocalDate weekStartDate,
    ScheduleStatus status,
    LocalDateTime publishedAt
) {

    public static SchedulePublishResponse from(
        Schedule schedule
    ) {
        return new SchedulePublishResponse(
            schedule.getId(),
            schedule.getWorkplace().getId(),
            schedule.getWeekStartDate(),
            schedule.getStatus(),
            schedule.getPublishedAt()
        );
    }
}