package com.merge.backend.domain.substitute.dto;

import com.merge.backend.domain.substitute.entity.RequestStatus;

public record SubstituteRequestCreateResponse(
    boolean requestCreated,
    Long requestId,
    Long shiftId,
    RequestStatus status,
    int candidateCount
) {

}
