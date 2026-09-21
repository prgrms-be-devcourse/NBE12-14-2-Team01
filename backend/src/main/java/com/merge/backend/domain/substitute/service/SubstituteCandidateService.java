package com.merge.backend.domain.substitute.service;

import com.merge.backend.domain.shift.entity.ScheduleStatus;
import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.repository.ShiftRepository;
import com.merge.backend.domain.shift.repository.UnavailableTimeRepository;
import com.merge.backend.domain.substitute.entity.SubstituteCandidate;
import com.merge.backend.domain.substitute.entity.SubstituteRequest;
import com.merge.backend.domain.substitute.repository.SubstituteCandidateRepository;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;
import com.merge.backend.domain.workplace.repository.WorkplaceMemberRepository;
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
        LocalDateTime now = LocalDateTime.now(); // 현재 서버 시간

        return substituteCandidateRepository.findReceivedRequests(
            userId, // 누구에게 온 요청을 찾을지
            now     // 아직 시작하지 않은 Shift인지 판단할 기준 시간
        );
    }
}
