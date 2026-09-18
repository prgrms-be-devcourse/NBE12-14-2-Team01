package com.merge.backend.domain.shift.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record UnavailableTimeUpdateReqBody(
        @NotNull(message = "시작시간은 필수로 입력하셔야 합니다.")
        LocalDateTime startAt,

        @NotNull(message = "끝나는 시간은 필수로 입력하셔야 합니다.")
        LocalDateTime endAt,
        
        @NotNull
        boolean confirmOfficialShiftConflict
) {
}
