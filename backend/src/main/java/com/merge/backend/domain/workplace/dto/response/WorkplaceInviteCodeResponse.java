package com.merge.backend.domain.workplace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WorkplaceInviteCodeResponse {

    private Long workplaceId;
    private String inviteCode;
}