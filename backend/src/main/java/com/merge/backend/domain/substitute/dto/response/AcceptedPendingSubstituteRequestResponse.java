package com.merge.backend.domain.substitute.dto.response;

import com.merge.backend.domain.substitute.entity.CandidateStatus;
import com.merge.backend.domain.substitute.entity.RequestStatus;
import com.merge.backend.domain.substitute.entity.SubstituteCandidate;
import java.time.LocalDateTime;

public record AcceptedPendingSubstituteRequestResponse(
    Long candidateId,                // 내가 수락한 Candidate ID
    Long requestId,                  // 대타 요청 ID
    Long shiftId,                    // 대타 대상 Shift ID
    Long workplaceId,                // 근무지 ID
    String workplaceName,            // 근무지 이름
    String requesterName,            // 원래 대타 요청자 이름
    LocalDateTime startAt,           // 대타 근무 시작 시간
    LocalDateTime endAt,             // 대타 근무 종료 시간
    RequestStatus requestStatus,     // 여기서는 ACCEPTED
    CandidateStatus candidateStatus, // 여기서는 ACCEPTED
    LocalDateTime respondedAt         // 내가 수락한 시간
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