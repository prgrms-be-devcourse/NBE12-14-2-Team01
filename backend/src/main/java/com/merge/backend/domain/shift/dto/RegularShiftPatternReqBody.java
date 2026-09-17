package com.merge.backend.domain.shift.dto;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record RegularShiftPatternReqBody(
        @NotNull(message = "memberId는 필수로 입력해야 합니다.")
        Long memberId,

        @NotNull(message = "요일은 필수로 입력해야 합니다.")
        DayOfWeek dayOfWeek,

        @NotNull(message = "시작시간은 필수로 입력해야 합니다.")
        LocalTime startTime,

        @NotNull(message = "끝나는 시간은 필수로 입력해야 합니다.")
        LocalTime endTime
) {
}
