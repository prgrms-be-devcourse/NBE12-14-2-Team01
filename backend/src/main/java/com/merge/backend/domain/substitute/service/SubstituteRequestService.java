package com.merge.backend.domain.substitute.service;

import com.merge.backend.domain.shift.entity.ScheduleStatus;
import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.entity.ShiftStatus;
import com.merge.backend.domain.shift.exception.ScheduleErrorCode;
import com.merge.backend.domain.shift.exception.ShiftErrorCode;
import com.merge.backend.domain.shift.repository.ShiftRepository;
import com.merge.backend.domain.shift.repository.UnavailableTimeRepository;
import com.merge.backend.domain.substitute.dto.SubstituteRequestListResponse;
import com.merge.backend.domain.substitute.dto.response.SubstituteRequestCreateResponse;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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
    public SubstituteRequestCreateResponse create(
        Long shiftId,
        Long actorUserId
    ) {
        Shift shift = validateSubstituteRequest(shiftId, actorUserId);

        SubstituteRequest request = substituteRequestRepository.save(
            new SubstituteRequest(
                shift,
                shift.getMember(),
                RequestStatus.OPEN
            )
        );

        // TODO: 후보 계산 연결

        return new SubstituteRequestCreateResponse(
            true,
            request.getId(),
            shiftId,
            request.getStatus(),
            0
        );
    }

    private Shift validateSubstituteRequest(
        Long shiftId,
        Long actorUserId
    ) {
        Shift shift = shiftRepository.findById(shiftId)
            .orElseThrow(() ->
                new BusinessException(
                    SubstituteErrorCode.SHIFT_NOT_FOUND
                )
            );

        if (shift.getSchedule().getStatus()
            != ScheduleStatus.PUBLISHED) {

            throw new BusinessException(
                SubstituteErrorCode.SHIFT_NOT_PUBLISHED
            );
        }

        if (shift.getStatus() != ShiftStatus.SCHEDULED) {
            throw new BusinessException(
                SubstituteErrorCode.SHIFT_CANCELLED
            );
        }

        if (!shift.getMember()
            .getUser()
            .getId()
            .equals(actorUserId)) {

            throw new BusinessException(
                SubstituteErrorCode.NOT_OWN_SHIFT
            );
        }

        if (!shift.getStartAt()
            .isAfter(LocalDateTime.now(clock))) {

            throw new BusinessException(
                SubstituteErrorCode.SHIFT_ALREADY_STARTED
            );
        }

        if (substituteRequestRepository
            .existsByShift_IdAndStatusIn(
                shiftId,
                List.of(
                    RequestStatus.OPEN,
                    RequestStatus.ACCEPTED
                )
            )) {

            throw new BusinessException(
                SubstituteErrorCode.ACTIVE_REQUEST_EXISTS
            );
        }

        return shift;
    }

    @Transactional(readOnly = true)
    public List<SubstituteRequestListResponse> list(
        Long workplaceId,
        Long actorId
    ) {

        Workplace workplace =
            workplaceRepository.findById(workplaceId)
                .orElseThrow(() ->
                    new BusinessException(
                        WorkplaceErrorCode.WORKPLACE_NOT_FOUND
                    )
                );

        workplaceMemberService.requireManager(
            actorId,
            workplaceId
        );

        // Request 조회
        List<SubstituteRequest> requests =
            substituteRequestRepository.findAllByWorkplaceId(
                workplaceId,
                LocalDateTime.now(clock)
            );

        // ACCEPTED 상태 Request의 ID만 가져오기
        List<Long> acceptedRequestIds =
            requests.stream()
                .filter(request ->
                    request.getStatus()
                        == RequestStatus.ACCEPTED
                )
                .map(SubstituteRequest::getId)
                .toList();

        Map<Long, SubstituteCandidate> acceptedCandidateMap =
            getAcceptedCandidates(acceptedRequestIds);

        return requests.stream()
            .map(request ->
                SubstituteRequestListResponse.from(
                    request,
                    acceptedCandidateMap
                )
            )
            .toList();
    }

    // ACCEPTED Request에 연결된 수락 Candidate 조회
    private Map<Long, SubstituteCandidate> getAcceptedCandidates(
        List<Long> requestIds
    ) {

        if (requestIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<SubstituteCandidate> candidates =
            substituteCandidateRepository
                .findByRequestIdInAndStatus(
                    requestIds,
                    CandidateStatus.ACCEPTED
                );

        // requestId별 Candidate 그룹화
        Map<Long, List<SubstituteCandidate>> grouped =
            candidates.stream()
                .collect(
                    Collectors.groupingBy(
                        candidate ->
                            candidate.getRequest().getId()
                    )
                );

        // 한 Request에 ACCEPTED Candidate가 여러 명이면 비정상 상태
        grouped.forEach(
            (requestId, matchedCandidates) -> {

                if (matchedCandidates.size() > 1) {
                    log.error(
                        "ACCEPTED 요청(requestId={})에 수락자가 {}명 조회되었습니다. candidateIds={}",
                        requestId,
                        matchedCandidates.size(),
                        matchedCandidates.stream()
                            .map(SubstituteCandidate::getId)
                            .toList()
                    );

                    throw new BusinessException(
                        SubstituteRequestErrorCode
                            .ACCEPTED_CANDIDATE_DUPLICATED
                    );
                }
            }
        );

        // ACCEPTED Request인데 ACCEPTED Candidate가 없는 경우 확인
        List<Long> missingRequestIds =
            requestIds.stream()
                .filter(requestId ->
                    !grouped.containsKey(requestId)
                )
                .toList();

        if (!missingRequestIds.isEmpty()) {
            log.error(
                "ACCEPTED 상태인 요청의 수락자 정보가 누락되었습니다. requestIds={}",
                missingRequestIds
            );

            throw new BusinessException(
                SubstituteRequestErrorCode
                    .ACCEPTED_CANDIDATE_NOT_FOUND
            );
        }

        return grouped.entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().get(0)
                )
            );
    }

    @Transactional
    public SubstituteRequest approve(
        Long requestId,
        Long actorId
    ) {

        // 대타 요청 존재 확인
        SubstituteRequest request =
            substituteRequestRepository
                .findById(requestId)
                .orElseThrow(() ->
                    new BusinessException(
                        SubstituteRequestErrorCode
                            .SUBSTITUTE_REQUEST_NOT_FOUND
                    )
                );

        // Request가 ACCEPTED 상태인지 확인
        if (request.getStatus()
            != RequestStatus.ACCEPTED) {

            throw new BusinessException(
                SubstituteRequestErrorCode.INVALID_REQUEST
            );
        }

        // 대상 Workplace ID
        Long workplaceId =
            request.getShift()
                .getSchedule()
                .getWorkplace()
                .getId();

        // 현재 사용자가 해당 Workplace의 MANAGER인지 확인
        workplaceMemberService.requireManager(
            actorId,
            workplaceId
        );

        // Schedule이 공개 상태인지 확인
        if (request.getShift()
            .getSchedule()
            .getStatus()
            != ScheduleStatus.PUBLISHED) {

            throw new BusinessException(
                ScheduleErrorCode.NOT_PUBLISHED
            );
        }

        // Shift가 정상 상태인지 확인
        if (request.getShift().getStatus()
            != ShiftStatus.SCHEDULED) {

            throw new BusinessException(
                ShiftErrorCode.IS_CANCELED_SHIFT
            );
        }

        // 원래 요청자가 아직 Shift 담당자인지 확인
        if (!Objects.equals(
            request.getRequesterMember().getId(),
            request.getShift().getMember().getId()
        )) {

            throw new BusinessException(
                SubstituteRequestErrorCode.INVALID_MEMBER
            );
        }

        Workplace workplace =
            workplaceRepository.findById(workplaceId)
                .orElseThrow(() ->
                    new BusinessException(
                        WorkplaceErrorCode.WORKPLACE_NOT_FOUND
                    )
                );

        // ACCEPTED Candidate 조회
        SubstituteCandidate candidate =
            substituteCandidateRepository
                .findByRequestIdAndStatus(
                    requestId,
                    CandidateStatus.ACCEPTED
                )
                .orElseThrow(() ->
                    new BusinessException(
                        SubstituteRequestErrorCode
                            .NOT_FOUND_CANDIDATE
                    )
                );

        WorkplaceMember acceptedMember =
            candidate.getMember();

        // 수락자가 아직 해당 Workplace 소속인지 확인
        if (!Objects.equals(
            acceptedMember.getWorkplace().getId(),
            workplaceId
        ) || acceptedMember.getLeftAt() != null) {

            throw new BusinessException(
                ShiftErrorCode
                    .INVALID_WORKPLACE_MEMBER_VALUE
            );
        }

        // 수락 이후 공식 근무가 새로 생겼는지 확인
        boolean hasConflictingShift =
            shiftRepository.existsConflictingShift(
                candidate.getMember()
                    .getUser()
                    .getId(),
                request.getShift().getStartAt(),
                request.getShift().getEndAt()
            );

        if (hasConflictingShift) {
            throw new BusinessException(
                SubstituteRequestErrorCode.CONFLICT_SHIFT
            );
        }

        // Shift 시작 시간이 지나지 않았는지 확인
        LocalDateTime validationNow =
            LocalDateTime.now(clock);

        validateNotStarted(
            request.getShift(),
            validationNow
        );

        // UnavailableTime 충돌 확인
        if (unavailableTimeRepository
            .existsOverlappingUnavailableTime(
                candidate.getMember()
                    .getUser()
                    .getId(),
                request.getShift().getStartAt(),
                request.getShift().getEndAt()
            )) {

            throw new BusinessException(
                SubstituteRequestErrorCode
                    .CONFLICT_UNAVAILABLE_TIME
            );
        }

        // 다른 활성 대타 수락 약속 충돌 확인
        if (substituteCandidateRepository
            .existsConflictingActiveSubstitute(
                candidate.getMember()
                    .getUser()
                    .getId(),
                requestId,
                request.getShift().getStartAt(),
                request.getShift().getEndAt(),
                validationNow
            )) {

            throw new BusinessException(
                SubstituteRequestErrorCode
                    .CONFLICT_ACTIVE_SUBSTITUTE
            );
        }

        // 실제 승인 직전 시간 다시 확인
        LocalDateTime approvedAt =
            LocalDateTime.now(clock);

        validateNotStarted(
            request.getShift(),
            approvedAt
        );

        // 최종 승인 및 Shift 담당자 변경
        request.approveRequest(
            candidate.getMember(),
            approvedAt
        );

        // TODO: 요청자와 수락자에게 알림 주기

        return request;
    }

    private void validateNotStarted(
        Shift shift,
        LocalDateTime now
    ) {
        if (!shift.getStartAt().isAfter(now)) {
            throw new BusinessException(
                ShiftErrorCode.INVALID_TIME_VALUE
            );
        }
    }
}