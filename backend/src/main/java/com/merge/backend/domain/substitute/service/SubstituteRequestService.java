package com.merge.backend.domain.substitute.service;

import com.merge.backend.domain.shift.entity.ScheduleStatus;
import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.entity.ShiftStatus;
import com.merge.backend.domain.shift.exception.ScheduleErrorCode;
import com.merge.backend.domain.shift.exception.ShiftErrorCode;
import com.merge.backend.domain.shift.repository.ShiftRepository;
import com.merge.backend.domain.shift.repository.UnavailableTimeRepository;
import com.merge.backend.domain.substitute.dto.SubstituteRequestCreateResponse;
import com.merge.backend.domain.substitute.entity.CandidateStatus;
import com.merge.backend.domain.substitute.entity.RequestStatus;
import com.merge.backend.domain.substitute.entity.SubstituteCandidate;
import com.merge.backend.domain.substitute.entity.SubstituteRequest;
import com.merge.backend.domain.substitute.exception.SubstituteErrorCode;
import com.merge.backend.domain.substitute.exception.SubstituteRequestErrorCode;
import com.merge.backend.domain.substitute.repository.SubstituteCandidateRepository;
import com.merge.backend.domain.substitute.repository.SubstituteRequestRepository;
import com.merge.backend.domain.workplace.entity.Workplace;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.exception.WorkplaceErrorCode;
import com.merge.backend.domain.workplace.repository.WorkplaceRepository;
import com.merge.backend.domain.workplace.service.WorkplaceMemberService;
import com.merge.backend.global.exception.BusinessException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubstituteRequestService {

    private final SubstituteRequestRepository substituteRequestRepository;
    private final SubstituteCandidateRepository substituteCandidateRepository;
    private final ShiftRepository shiftRepository;
    private final WorkplaceRepository workplaceRepository;
    private final UnavailableTimeRepository unavailableTimeRepository;
    private final WorkplaceMemberService workplaceMemberService;
    private final Clock clock;

    @Transactional
    public SubstituteRequestCreateResponse create(Long shiftId, Long actorUserId) {
        Shift shift = validateSubstituteRequest(shiftId, actorUserId);

        SubstituteRequest request = substituteRequestRepository.save(
            new SubstituteRequest(shift, shift.getMember(), RequestStatus.OPEN)
        );

        // TODO: 후보 계산 연결

        return new SubstituteRequestCreateResponse(true, request.getId(), shiftId,
            request.getStatus(), 0);
    }

    private Shift validateSubstituteRequest(Long shiftId, Long actorUserId) {
        Shift shift = shiftRepository.findById(shiftId)
            .orElseThrow(() -> new
                BusinessException(SubstituteErrorCode.SHIFT_NOT_FOUND));

        if (shift.getSchedule().getStatus() != ScheduleStatus.PUBLISHED) {
            throw new BusinessException(SubstituteErrorCode.SHIFT_NOT_PUBLISHED);
        }

        if (shift.getStatus() != ShiftStatus.SCHEDULED) {
            throw new BusinessException(SubstituteErrorCode.SHIFT_CANCELLED);
        }

        if (!shift.getMember().getUser().getId().equals(actorUserId)) {
            throw new BusinessException(SubstituteErrorCode.NOT_OWN_SHIFT);
        }

        if (!shift.getStartAt().isAfter(LocalDateTime.now(clock))) {
            throw new BusinessException(SubstituteErrorCode.SHIFT_ALREADY_STARTED);
        }

        if (substituteRequestRepository.existsByShift_IdAndStatusIn(
            shiftId, List.of(RequestStatus.OPEN, RequestStatus.ACCEPTED))) {
            throw new BusinessException(SubstituteErrorCode.ACTIVE_REQUEST_EXISTS);
        }
        return shift;
    }

    @Transactional
    public SubstituteRequest approve(Long requestId, Long actorId) {

        //대체 근무 요청이 존재하는지
        SubstituteRequest request = substituteRequestRepository.findById(requestId)
            .orElseThrow(() ->
                new BusinessException(SubstituteRequestErrorCode.SUBSTITUTE_REQUEST_NOT_FOUND));

        //request == ACCEPTED 여부 검사
        if(request.getStatus() != RequestStatus.ACCEPTED) {
            throw new BusinessException(SubstituteRequestErrorCode.INVALID_REQUEST);
        }
        //근무지 ID 뽑아오기
        Long workplaceId = request.getShift().getSchedule().getWorkplace().getId();
        //매니저 인가 확인
        workplaceMemberService.requireManager(actorId, workplaceId);

        //요청의 스케줄이 공개되지 않았을 때
        if(request.getShift().getSchedule().getStatus() != ScheduleStatus.PUBLISHED) {
            throw new BusinessException(ScheduleErrorCode.NOT_PUBLISHED);
        }
        //요청의 근무가 취소되었다면
        if(request.getShift().getStatus() != ShiftStatus.SCHEDULED) {
            throw new BusinessException(ShiftErrorCode.IS_CANCELED_SHIFT);
        }
        //현재 Shift.member != Request.requesterMember
        if (!Objects.equals(request.getRequesterMember().getId(),
            request.getShift().getMember().getId())
        ) {
            throw new BusinessException(SubstituteRequestErrorCode.INVALID_MEMBER);
        }
        Workplace workplace = workplaceRepository.findById(workplaceId)
            .orElseThrow(() ->
                new BusinessException(WorkplaceErrorCode.WORKPLACE_NOT_FOUND));

        SubstituteCandidate candidate = substituteCandidateRepository.findByRequestIdAndStatus(
            requestId, CandidateStatus.ACCEPTED)
            .orElseThrow(() ->
                new BusinessException(SubstituteRequestErrorCode.NOT_FOUND_CANDIDATE));

        //수락자가 해당 근무지 소속인지
        WorkplaceMember acceptedMember = candidate.getMember();

        if (!Objects.equals(acceptedMember.getWorkplace().getId(), workplaceId) ||
            acceptedMember.getLeftAt() != null
        ) {
            throw new BusinessException(ShiftErrorCode.INVALID_WORKPLACE_MEMBER_VALUE);
        }
        //혹여나 수락 후 승인 전 사이에 근무를 배정받았는지
        boolean hasConflictingShift = shiftRepository.existsConflictingShift(
            candidate.getMember().getUser().getId(),
            request.getShift().getStartAt(), // 시작 시간
            request.getShift().getEndAt()// 종료 시간
        );
        if(hasConflictingShift){
            throw new BusinessException(SubstituteRequestErrorCode.CONFLICT_SHIFT);
        }
        //현재를 기준으로 대체 근무 시작 시간이 지나거나 같다면 (now >= startAt)
        LocalDateTime validationNow = LocalDateTime.now(clock);
        validateNotStarted(request.getShift(), validationNow);

        //불가능 시간과 중복되는지
        if(unavailableTimeRepository.existsOverlappingUnavailableTime(
            candidate.getMember().getUser().getId(),
            request.getShift().getStartAt(),
            request.getShift().getEndAt()
        )){
            throw new BusinessException(SubstituteRequestErrorCode.CONFLICT_UNAVAILABLE_TIME);
        }
        //이전에 다른 대체 근무를 수락했다면, 그것과 중복되는지
        if(substituteCandidateRepository.existsConflictingActiveSubstitute(
            candidate.getMember().getUser().getId(), requestId,
            request.getShift().getStartAt(), request.getShift().getEndAt(), validationNow)
        ){
            throw new BusinessException(SubstituteRequestErrorCode.CONFLICT_ACTIVE_SUBSTITUTE);
        }
        //승인 전 시간 재확인
        LocalDateTime approvedAt = LocalDateTime.now(clock);
        validateNotStarted(request.getShift(), approvedAt);

        //최종 승인 시작
        request.approveRequest(candidate.getMember(),approvedAt);

        ///todo: 요청자와 수락자에게 알림 주기

        return request;
    }
    private void validateNotStarted(Shift shift, LocalDateTime now) {
        if (!shift.getStartAt().isAfter(now)) {
            throw new BusinessException(ShiftErrorCode.INVALID_TIME_VALUE);
        }
    }
}
