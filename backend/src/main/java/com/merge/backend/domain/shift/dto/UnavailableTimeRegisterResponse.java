package com.merge.backend.domain.shift.dto;

import java.time.LocalDateTime;

public record UnavailableTimeRegisterResponse(
        Long unavailableTimeid,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
}
