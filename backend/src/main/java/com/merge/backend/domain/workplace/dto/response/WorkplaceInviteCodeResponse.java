package com.merge.backend.domain.workplace.dto.response;

import com.merge.backend.domain.workplace.entity.Workplace;

public record WorkplaceInviteCodeResponse(
    Long workplaceId,
    String inviteCode
) {

    public static WorkplaceInviteCodeResponse from(Workplace workplace) {
        return new WorkplaceInviteCodeResponse(
            workplace.getId(),
            workplace.getInviteCode()
        );
    }
}