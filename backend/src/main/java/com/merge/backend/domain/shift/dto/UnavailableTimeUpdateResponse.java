package com.merge.backend.domain.shift.dto;

import java.time.LocalDateTime;

public record UnavailableTimeUpdateResponse(
        Long unavailableTimeId,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
}
