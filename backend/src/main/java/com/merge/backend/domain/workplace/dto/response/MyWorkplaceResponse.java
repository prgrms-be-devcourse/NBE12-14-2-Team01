package com.merge.backend.domain.workplace.dto.response;

import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;

public record MyWorkplaceResponse(
    Long workplaceId,
    String name,
    WorkplaceRole role
) {

    public static MyWorkplaceResponse from(WorkplaceMember member) {
        return new MyWorkplaceResponse(
            member.getWorkplace().getId(),
            member.getWorkplace().getName(),
            member.getRole()
        );
    }

}