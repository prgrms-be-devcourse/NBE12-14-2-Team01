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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.NegativeValidatorForNumber;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubstituteCandidateService {

    private final WorkplaceMemberRepository workplaceMemberRepository;
    private final ShiftRepository shiftRepository;
    private final UnavailableTimeRepository unavailableTimeRepository;
    private final SubstituteCandidateRepository substituteCandidateRepository;
    private final NegativeValidatorForNumber negativeValidatorForNumber;

    @Transactional(readOnly = true)
    public List<WorkplaceMember> findCadidates(Long workplaceId, Long requestMemberId,
        Shift targetShift) {
        return workplaceMemberRepository.findAllByWorkplace_IdAndLeftAtIsNull(workplaceId).stream()
            .filter(member -> member.getRole() == WorkplaceRole.EMPLOYEE)
            .filter(member -> !member.getRole().equals(requestMemberId)).filter(
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
}
