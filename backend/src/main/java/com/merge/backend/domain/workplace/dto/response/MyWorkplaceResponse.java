package com.merge.backend.domain.workplace.dto.response;

import com.merge.backend.domain.workplace.entity.WorkplaceRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MyWorkplaceResponse {

    private Long workplaceId;
    private String name;
    private WorkplaceRole role;
}