package com.merge.backend.domain.shift.dto;

import com.merge.backend.domain.workplace.entity.WorkplaceRole;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record RegularShiftPatternListResponse(
        Long patternId,
        Long memberId,
        String memberName,
        WorkplaceRole role,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
}
