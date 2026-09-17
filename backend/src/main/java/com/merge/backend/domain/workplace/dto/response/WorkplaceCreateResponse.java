package com.merge.backend.domain.workplace.dto.response;

import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;

public record WorkplaceCreateResponse(
    Long workplaceId,
    String name,
    WorkplaceRole role
) {

    public static WorkplaceCreateResponse from(WorkplaceMember member) {
        return new WorkplaceCreateResponse(
            member.getWorkplace().getId(),
            member.getWorkplace().getName(),
            member.getRole()
        );
    }

}
