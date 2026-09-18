package com.merge.backend.domain.workplace.dto.request;

import jakarta.validation.constraints.NotBlank;

public record WorkplaceJoinRequest(
    @NotBlank String inviteCode
) {

}
