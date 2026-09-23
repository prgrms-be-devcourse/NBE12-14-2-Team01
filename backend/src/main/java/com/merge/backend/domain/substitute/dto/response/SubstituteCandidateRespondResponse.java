package com.merge.backend.domain.substitute.dto.response;

import com.merge.backend.domain.substitute.entity.CandidateStatus;
import com.merge.backend.domain.substitute.entity.RequestCloseReason;
import com.merge.backend.domain.substitute.entity.RequestStatus;
import com.merge.backend.domain.substitute.entity.SubstituteCandidate;
import java.time.LocalDateTime;

public record SubstituteCandidateRespondResponse(

    Long candidateId,              // 응답한 Candidate ID
    Long requestId,                // 연결된 대타 요청 ID
    CandidateStatus candidateStatus, // ACCEPTED 또는 REJECTED
    RequestStatus requestStatus,     // 처리 후 Request 상태
    LocalDateTime respondedAt,       // 수락/거절한 시간
    RequestCloseReason closeReason   // 마지막 거절로 CLOSED된 경우 종료 사유
) {

    public static SubstituteCandidateRespondResponse from(
        SubstituteCandidate candidate
    ) {
        return new SubstituteCandidateRespondResponse(
            candidate.getId(),
            candidate.getRequest().getId(),
            candidate.getStatus(),
            candidate.getRequest().getStatus(),
            candidate.getRespondedAt(),
            candidate.getRequest().getCloseReason()
        );
    }
}