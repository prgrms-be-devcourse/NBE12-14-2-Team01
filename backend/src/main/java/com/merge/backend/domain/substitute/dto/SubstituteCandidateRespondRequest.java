package com.merge.backend.domain.substitute.dto;

import jakarta.validation.constraints.NotNull;

public record SubstituteCandidateRespondRequest(

    @NotNull
    Decision decision // 사용자가 선택한 행동: 수락 또는 거절

) {

    public enum Decision {
        ACCEPT, // 대타 요청 수락
        REJECT  // 대타 요청 거절
    }
}