package com.merge.backend.domain.substitute.dto.response;

import com.merge.backend.domain.substitute.entity.CandidateStatus;
import com.merge.backend.domain.substitute.entity.RequestStatus;
import com.merge.backend.domain.substitute.entity.SubstituteCandidate;
import java.time.LocalDateTime;

public record ReceivedSubstituteRequestResponse(
    Long candidateId,              // 내 Candidate ID → SUB-03에서 수락/거절할 때 사용
    Long requestId,                // 대타 요청 ID
    Long shiftId,                  // 대타가 필요한 근무 ID
    Long workplaceId,              // 어느 근무지인지
    String workplaceName,          // 화면에 보여줄 근무지 이름
    Long requesterMemberId,        // 대타를 요청한 직원의 Member ID
    String requesterName,          // 대타 요청자 이름
    LocalDateTime startAt,         // 대타 근무 시작 시간
    LocalDateTime endAt,           // 대타 근무 종료 시간
    RequestStatus requestStatus,   // 요청 상태 (여기서는 OPEN)
    CandidateStatus candidateStatus, // 내 응답 상태 (여기서는 PENDING)
    LocalDateTime createdAt        // 대타 요청이 생성된 시간
) {

    public static ReceivedSubstituteRequestResponse from(
        SubstituteCandidate candidate // 조회된 Candidate 한 건
    ) {
        return new ReceivedSubstituteRequestResponse(
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
                .getId(),

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

            candidate.getRequest()
                .getCreateDate()
        );
    }
}