package com.merge.backend.domain.workplace.dto.response;

import com.merge.backend.domain.workplace.entity.WorkplaceRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WorkplaceMemberResponse {

    private Long memberId;
    private String name;
    private String email;
    private WorkplaceRole role;
}