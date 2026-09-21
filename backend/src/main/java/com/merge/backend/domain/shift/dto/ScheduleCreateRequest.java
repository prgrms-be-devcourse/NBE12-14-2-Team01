package com.merge.backend.domain.shift.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ScheduleCreateRequest(

    @NotNull(message = "주 시작일은 필수입니다.")
    LocalDate weekStartDate

) {
}
