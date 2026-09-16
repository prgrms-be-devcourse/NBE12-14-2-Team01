package com.merge.backend.domain.shift.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record RegularShiftPatternReqBody(
        Long memberId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
}
