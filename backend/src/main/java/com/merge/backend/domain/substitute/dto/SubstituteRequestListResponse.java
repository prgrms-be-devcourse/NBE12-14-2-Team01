package com.merge.backend.domain.substitute.dto;

import com.merge.backend.domain.substitute.entity.RequestStatus;
import com.merge.backend.domain.substitute.entity.SubstituteCandidate;
import com.merge.backend.domain.substitute.entity.SubstituteRequest;
import java.time.LocalDateTime;
import java.util.Map;

public record SubstituteRequestListResponse(
    Long requestId,
    Long shiftId,
    Long requesterMemberId,
    String requesterName,
    Long workplaceId,
    String workplaceName,
    LocalDateTime startAt,
    LocalDateTime endAt,
    RequestStatus status,
    LocalDateTime createdAt,
    AcceptedMemberResponse acceptedMember
){
    public static SubstituteRequestListResponse from(
        SubstituteRequest request,
        Map<Long, SubstituteCandidate> acceptedCandidateMap
    ) {
        AcceptedMemberResponse acceptedMember = null;

        // 수락자 정보 추출
        if (request.getStatus() == RequestStatus.ACCEPTED) {
            SubstituteCandidate candidate = acceptedCandidateMap.get(request.getId());
            if (candidate != null) {
                acceptedMember = new AcceptedMemberResponse(
                    candidate.getMember().getId(),
                    candidate.getMember().getUser().getName()
                );
            }
        }
        return new SubstituteRequestListResponse(
            request.getId(),
            request.getShift().getId(),
            request.getRequesterMember().getId(),
            request.getRequesterMember().getUser().getName(),
            request.getShift().getSchedule().getWorkplace().getId(),
            request.getShift().getSchedule().getWorkplace().getName(),
            request.getShift().getStartAt(),
            request.getShift().getEndAt(),
            request.getStatus(),
            request.getCreateDate(),
            acceptedMember
        );
    }
    // acceptedMember 응답 전용 규격 DTO
    public record AcceptedMemberResponse(
        Long memberId,
        String name
    ) {}
}

