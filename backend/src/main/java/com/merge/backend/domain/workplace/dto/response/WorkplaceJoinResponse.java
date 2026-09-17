package com.merge.backend.domain.workplace.dto.response;

import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;

public record WorkplaceJoinResponse(
    Long workplaceId,
    String name,
    WorkplaceRole role
) {

    public static WorkplaceJoinResponse from(WorkplaceMember member) {
        return new WorkplaceJoinResponse(
            member.getWorkplace().getId(),
            member.getWorkplace().getName(),
            member.getRole()
        );
    }

}
