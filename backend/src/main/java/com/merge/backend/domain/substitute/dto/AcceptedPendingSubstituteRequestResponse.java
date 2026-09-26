package com.merge.backend.domain.substitute.dto;

import com.merge.backend.domain.substitute.entity.CandidateStatus;
import com.merge.backend.domain.substitute.entity.RequestStatus;
import com.merge.backend.domain.substitute.entity.SubstituteCandidate;
import java.time.LocalDateTime;

public record AcceptedPendingSubstituteRequestResponse(
    Long candidateId,
    Long requestId,
    Long shiftId,
    Long workplaceId,
    String workplaceName,
    String requesterName,
    LocalDateTime startAt,
    LocalDateTime endAt,
    RequestStatus requestStatus,
    CandidateStatus candidateStatus,
    LocalDateTime respondedAt
) {

    public static AcceptedPendingSubstituteRequestResponse from(
        SubstituteCandidate candidate // 조회된 Candidate 한 건
    ) {
        return new AcceptedPendingSubstituteRequestResponse(
            candidate.getId(),

            candidate.getRequest().getId(),

            candidate.getRequest()
                .getShift()
                .getId(),

            candidate.getRequest()
                .getShift()
                .getSchedule()
                .getWorkplace()
                .getId(),

            candidate.getRequest()
                .getShift()
                .getSchedule()
                .getWorkplace()
                .getName(),

            candidate.getRequest()
                .getRequesterMember()
                .getUser()
                .getName(),

            candidate.getRequest()
                .getShift()
                .getStartAt(),

            candidate.getRequest()
                .getShift()
                .getEndAt(),

            candidate.getRequest()
                .getStatus(),

            candidate.getStatus(),

            candidate.getRespondedAt()
        );
    }
}