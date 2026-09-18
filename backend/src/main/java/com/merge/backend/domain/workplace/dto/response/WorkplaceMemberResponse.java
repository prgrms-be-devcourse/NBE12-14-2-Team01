package com.merge.backend.domain.workplace.dto.response;

import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;

public record WorkplaceMemberResponse(
    Long memberId,
    String name,
    String email,
    WorkplaceRole role
) {
    public static WorkplaceMemberResponse from(WorkplaceMember member) {
        return new WorkplaceMemberResponse(
            member.getId(),
            member.getUser().getName(),
            member.getUser().getEmail(),
            member.getRole()
        );
    }
}