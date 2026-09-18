package com.merge.backend.domain.workplace.dto.request;

import jakarta.validation.constraints.NotBlank;

public record WorkplaceCreateRequest(
    @NotBlank String name
) {

}