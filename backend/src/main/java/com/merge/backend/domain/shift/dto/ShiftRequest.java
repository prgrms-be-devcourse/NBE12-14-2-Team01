package com.merge.backend.domain.shift.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ShiftRequest (

    @NotNull Long memberId,
    @NotNull LocalDateTime startAt,
    @NotNull LocalDateTime endAt,
    @NotNull Boolean confirmUnavailableConflict
){

}
