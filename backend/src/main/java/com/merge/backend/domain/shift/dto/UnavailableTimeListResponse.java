package com.merge.backend.domain.shift.dto;

import java.time.LocalDateTime;

public record UnavailableTimeListResponse(
        Long unavailableTimeId,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean officialShiftConflict
) {
}
