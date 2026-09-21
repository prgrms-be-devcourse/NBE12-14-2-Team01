package com.merge.backend.domain.shift.dto;

import jakarta.validation.constraints.NotNull;

public record SchedulePublishRequest(

    @NotNull(message = "근무 불가능 일정 충돌 확인 여부는 필수입니다.")
    Boolean confirmUnavailableConflict

) {
}