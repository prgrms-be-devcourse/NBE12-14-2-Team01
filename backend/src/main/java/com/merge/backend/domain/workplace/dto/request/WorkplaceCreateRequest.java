package com.merge.backend.domain.workplace.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WorkplaceCreateRequest {

    @NotBlank
    private String name;

}
