package com.merge.backend.domain.shift.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record RegularShiftPatternResponse(
        Long patternId,
        Long memberId,
        String memberName,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
}
