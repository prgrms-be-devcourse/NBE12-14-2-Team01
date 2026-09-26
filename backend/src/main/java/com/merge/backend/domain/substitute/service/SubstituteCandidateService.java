package com.merge.backend.domain.substitute.service;

import com.merge.backend.domain.shift.entity.ScheduleStatus;
import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.entity.ShiftStatus;
import com.merge.backend.domain.shift.repository.ShiftRepository;
import com.merge.backend.domain.shift.repository.UnavailableTimeRepository;
import com.merge.backend.domain.substitute.entity.CandidateStatus;
import com.merge.backend.domain.substitute.entity.RequestStatus;
import com.merge.backend.domain.substitute.entity.SubstituteCandidate;
import com.merge.backend.domain.substitute.entity.SubstituteRequest;
import com.merge.backend.domain.substitute.exception.SubstituteRequestErrorCode;
import com.merge.backend.domain.substitute.repository.SubstituteCandidateRepository;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;
import com.merge.backend.domain.workplace.repository.WorkplaceMemberRepository;
import com.merge.backend.global.exception.BusinessException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubstituteCandidateService {

    private final WorkplaceMemberRepository workplaceMemberRepository;
    private final ShiftRepository shiftRepository;
    private final UnavailableTimeRepository unavailableTimeRepository;
    private final SubstituteCandidateRepository substituteCandidateRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<WorkplaceMember> findCandidates(Long workplaceId, Long requestMemberId,
        Shift targetShift) {
        return workplaceMemberRepository.findAllByWorkplace_IdAndLeftAtIsNull(workplaceId)
            .stream()
            .filter(member -> member.getRole() == WorkplaceRole.EMPLOYEE)
            .filter(member -> !member.getId().equals(requestMemberId))
            .filter(
                member -> !shiftRepository.existsOverlappingOfficialShift(
                    member.getUser().getId(),
                    ScheduleStatus.PUBLISHED,
                    targetShift.getStartAt(),
                    targetShift.getEndAt(),
                    targetShift.getId()))
            .filter(member -> !unavailableTimeRepository.existsOverlappingUnavailableTime(
                member.getUser().getId(),
                targetShift.getStartAt(),
                targetShift.getEndAt()))
            .filter(member -> !substituteCandidateRepository.existsOverlappingAcceptedSubstitute(
                member.getUser().getId(),
                targetShift.getStartAt(),
                targetShift.getEndAt()))
            .toList();
    }

    public List<SubstituteCandidate> createCandidates(SubstituteRequest request,
        List<WorkplaceMember> members) {
        List<SubstituteCandidate> candidates = members.stream()
            .map(member -> new SubstituteCandidate(request, member)).toList();

        return substituteCandidateRepository.saveAll(candidates);
    }

    @Transactional(readOnly = true)
    public List<SubstituteCandidate> findReceivedRequests(
        Long userId // 현재 로그인한 User의 ID
    ) {
        LocalDateTime now = LocalDateTime.now(clock); // 현재 서버 시간

        return substituteCandidateRepository.findReceivedRequests(
            userId, // 누구에게 온 요청을 찾을지
            now     // 아직 시작하지 않은 Shift인지 판단할 기준 시간
        );
    }

    @Transactional(readOnly = true)
    public List<SubstituteCandidate> findAcceptedPendingRequests(
        Long userId // 현재 로그인한 User의 ID
    ) {
        LocalDateTime now = LocalDateTime.now(clock); // 현재 서버 시간

        return substituteCandidateRepository.findAcceptedPendingRequests(
            userId, // 내가 수락한 Candidate인지 확인할 User ID
            now     // 아직 시작하지 않은 Shift인지 판단할 기준 시간
        );
    }

    private SubstituteCandidate validateRespondableCandidate(
        Long candidateId, // 응답하려는 대타 후보 ID
        Long userId,      // 현재 로그인한 User ID
        LocalDateTime now // Clock으로 구한 현재 시간
    ) {
        SubstituteCandidate candidate = substituteCandidateRepository.findById(candidateId)
            .orElseThrow(() ->
                new BusinessException(SubstituteRequestErrorCode.CANDIDATE_NOT_FOUND)
            );

        WorkplaceMember member = candidate.getMember();

        // 이 Candidate가 현재 로그인한 사용자의 것인지 확인
        if (!member.getUser().getId().equals(userId)) {
            throw new BusinessException(SubstituteRequestErrorCode.NOT_OWN_CANDIDATE);
        }

        // 현재도 이 Workplace에 소속된 EMPLOYEE인지 확인
        if (member.getLeftAt() != null || member.getRole() != WorkplaceRole.EMPLOYEE) {
            throw new BusinessException(SubstituteRequestErrorCode.INVALID_CANDIDATE_MEMBER);
        }

        // 아직 응답하지 않은 Candidate인지 확인
        if (candidate.getStatus() != CandidateStatus.PENDING) {
            throw new BusinessException(SubstituteRequestErrorCode.CANDIDATE_ALREADY_RESPONDED);
        }

        SubstituteRequest request = candidate.getRequest();

        // 다른 Candidate가 먼저 수락했거나 이미 요청이 종료됐는지 확인
        if (request.getStatus() != RequestStatus.OPEN) {
            throw new BusinessException(SubstituteRequestErrorCode.REQUEST_NOT_OPEN);
        }

        Shift shift = request.getShift();

        // 공개된 근무표의 Shift인지 확인
        if (shift.getSchedule().getStatus() != ScheduleStatus.PUBLISHED) {
            throw new BusinessException(SubstituteRequestErrorCode.SHIFT_NOT_PUBLISHED);
        }

        // 취소되지 않은 정상 Shift인지 확인
        if (shift.getStatus() != ShiftStatus.SCHEDULED) {
            throw new BusinessException(SubstituteRequestErrorCode.SHIFT_CANCELLED);
        }

        // 이미 시작된 Shift에는 응답할 수 없음
        if (!shift.getStartAt().isAfter(now)) {
            throw new BusinessException(SubstituteRequestErrorCode.SHIFT_ALREADY_STARTED);
        }

        return candidate;
    }

    @Transactional
    public SubstituteCandidate acceptCandidate(
        Long candidateId, // 수락할 Candidate ID
        Long userId       // 현재 로그인한 User ID
    ) {
        LocalDateTime now = LocalDateTime.now(clock);

        // 수락/거절 공통 조건 먼저 검사
        SubstituteCandidate candidate =
            validateRespondableCandidate(candidateId, userId, now);

        Shift targetShift = candidate.getRequest().getShift();
        Long candidateUserId = candidate.getMember().getUser().getId();

        // 1. 다른 공식 근무와 시간이 겹치는지 확인
        if (shiftRepository.existsOverlappingOfficialShift(
            candidateUserId,
            ScheduleStatus.PUBLISHED,
            targetShift.getStartAt(),
            targetShift.getEndAt(),
            targetShift.getId()
        )) {
            throw new BusinessException(
                SubstituteRequestErrorCode.CONFLICT_SHIFT
            );
        }

        // 2. 근무 불가능 시간과 겹치는지 확인
        if (unavailableTimeRepository.existsOverlappingUnavailableTime(
            candidateUserId,
            targetShift.getStartAt(),
            targetShift.getEndAt()
        )) {
            throw new BusinessException(
                SubstituteRequestErrorCode.CONFLICT_UNAVAILABLE_TIME
            );
        }

        // 3. 이미 수락해둔 다른 대타 근무와 겹치는지 확인
        if (substituteCandidateRepository.existsOverlappingAcceptedSubstitute(
            candidateUserId,
            targetShift.getStartAt(),
            targetShift.getEndAt()
        )) {
            throw new BusinessException(
                SubstituteRequestErrorCode.CONFLICT_ACTIVE_SUBSTITUTE
            );
        }

        // Candidate: PENDING → ACCEPTED
        candidate.accept(now);

        // Request: OPEN → ACCEPTED
        candidate.getRequest().accept();

        return candidate;
    }

    @Transactional
    public SubstituteCandidate rejectCandidate(
        Long candidateId, // 거절할 Candidate ID
        Long userId       // 현재 로그인한 User ID
    ) {
        LocalDateTime now = LocalDateTime.now(clock);

        // 수락/거절 공통 조건 확인
        SubstituteCandidate candidate =
            validateRespondableCandidate(candidateId, userId, now);

        SubstituteRequest request = candidate.getRequest();

        // Candidate: PENDING → REJECTED
        candidate.reject(now);

        // 현재 Candidate를 제외하고 다른 PENDING 후보가 남아 있는지 확인
        boolean hasOtherPendingCandidate =
            substituteCandidateRepository.existsByRequest_IdAndStatusAndIdNot(
                request.getId(),
                CandidateStatus.PENDING,
                candidate.getId()
            );

        // 다른 PENDING 후보가 없다면 마지막 후보가 거절한 것
        if (!hasOtherPendingCandidate) {
            request.closeAllCandidatesRejected(now);
        }

        return candidate;
    }
}