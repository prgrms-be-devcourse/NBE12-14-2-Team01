package com.merge.backend.domain.shift.service;

import com.merge.backend.domain.shift.dto.RegularShiftPatternListResponse;
import com.merge.backend.domain.shift.dto.RegularShiftPatternReqBody;
import com.merge.backend.domain.shift.dto.RegularShiftPatternResponse;
import com.merge.backend.domain.shift.entity.RegularShiftPattern;
import com.merge.backend.domain.shift.exception.RegularShiftErrorCode;
import com.merge.backend.domain.shift.repository.RegularShiftPatternRepository;
import com.merge.backend.domain.workplace.repository.WorkplaceRepository;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;
import com.merge.backend.domain.workplace.repository.WorkplaceMemberRepository;
import com.merge.backend.global.exception.BusinessException;
import com.merge.backend.global.rq.Rq;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RegularShiftPatternService {

    private final RegularShiftPatternRepository regularShiftPatternRepository;
    private final WorkplaceMemberRepository workplaceMemberRepository;
    private final WorkplaceRepository workplaceRepository;
    private final Rq rq;

    public RegularShiftPatternResponse register(Long workplaceId,
                                                RegularShiftPatternReqBody reqBody) {
        //workplace 존재 확인
        validateWorkplaceExists(workplaceId);
        requireManager(workplaceId);

        //member 조회
        WorkplaceMember workplaceMember = getActiveMember(workplaceId, reqBody.memberId());

        //끝시간 확인
        validateTime(reqBody);

        //겹치는 시간 확인
        validateOverlap(reqBody, null);

        RegularShiftPattern pattern = new RegularShiftPattern(
                workplaceMember,
                reqBody.dayOfWeek(),
                reqBody.startTime(),
                reqBody.endTime()
        );

        RegularShiftPattern savedPattern = regularShiftPatternRepository.save(pattern);

        return new RegularShiftPatternResponse(
                savedPattern.getId(),
                workplaceMember.getId(),
                workplaceMember.getUser().getName(),
                savedPattern.getDayOfWeek(),
                savedPattern.getStartTime(),
                savedPattern.getEndTime()
        );

    }

    public RegularShiftPatternResponse update(
            Long workplaceId
            , Long patternId
            , RegularShiftPatternReqBody reqBody
    ) {

        validateWorkplaceExists(workplaceId);

        requireManager(workplaceId);

        RegularShiftPattern pattern = getPatternInWorkplace(workplaceId, patternId);

        WorkplaceMember member = getActiveMember(workplaceId, reqBody.memberId());

        validateTime(reqBody);

        validateOverlap(reqBody, patternId);

        pattern.update(
                member,
                reqBody.dayOfWeek(),
                reqBody.startTime(),
                reqBody.endTime()
        );

        return new RegularShiftPatternResponse(
                pattern.getId(),
                member.getId(),
                member.getUser().getName(),
                pattern.getDayOfWeek(),
                pattern.getStartTime(),
                pattern.getEndTime()
        );


    }


    public List<RegularShiftPatternListResponse> findAll(Long workplaceId) {

        validateWorkplaceExists(workplaceId);

        requireManager(workplaceId);

        List<RegularShiftPattern> patterns =
                regularShiftPatternRepository
                        .findByMemberWorkplaceIdAndMemberLeftAtIsNull(workplaceId);

        return patterns.stream()
                .map(pattern -> new RegularShiftPatternListResponse(
                        pattern.getId(),
                        pattern.getMember().getId(),
                        pattern.getMember().getUser().getName(),
                        pattern.getMember().getRole(),
                        pattern.getDayOfWeek(),
                        pattern.getStartTime(),
                        pattern.getEndTime()
                ))
                .toList();

    }

    public void delete(Long workplaceId, Long patternId) {

        validateWorkplaceExists(workplaceId);

        requireManager(workplaceId);

        RegularShiftPattern pattern = getPatternInWorkplace(workplaceId, patternId);

        regularShiftPatternRepository.delete(pattern);

    }

    private void validateWorkplaceExists(Long workplaceId) {
        if (!workplaceRepository.existsById(workplaceId)) {
            throw new BusinessException(
                    RegularShiftErrorCode.WORKPLACE_NOT_FOUND
            );
        }
    }

    private WorkplaceMember getActiveMember(Long workplaceId, Long memberId) {
        WorkplaceMember member =
                workplaceMemberRepository.findById(memberId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        RegularShiftErrorCode.MEMBER_NOT_FOUND
                                )
                        );

        if (!member.getWorkplace().getId().equals(workplaceId)) {
            throw new BusinessException(
                    RegularShiftErrorCode.MEMBER_NOT_IN_WORKPLACE
            );
        }

        if (member.getLeftAt() != null) {
            throw new BusinessException(
                    RegularShiftErrorCode.MEMBER_NOT_ACTIVE
            );
        }

        return member;
    }

    private RegularShiftPattern getPatternInWorkplace(
            Long workplaceId,
            Long patternId
    ) {
        RegularShiftPattern pattern =
                regularShiftPatternRepository.findById(patternId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        RegularShiftErrorCode.PATTERN_NOT_FOUND
                                )
                        );

        if (!pattern.getMember()
                .getWorkplace()
                .getId()
                .equals(workplaceId)) {

            throw new BusinessException(
                    RegularShiftErrorCode.PATTERN_NOT_IN_WORKPLACE
            );
        }

        return pattern;
    }

    private void validateTime(RegularShiftPatternReqBody reqBody) {
        if (!reqBody.startTime().isBefore(reqBody.endTime())) {
            throw new BusinessException(
                    RegularShiftErrorCode.INVALID_PATTERN_TIME
            );
        }
    }

    private void validateOverlap(RegularShiftPatternReqBody reqBody, Long excludePatternId) {
        List<RegularShiftPattern> patterns =
                regularShiftPatternRepository
                        .findByMemberIdAndDayOfWeek(
                                reqBody.memberId(),
                                reqBody.dayOfWeek()
                        );

        boolean overlap = patterns.stream()
                .filter(pattern ->
                        excludePatternId == null
                                || !pattern.getId().equals(excludePatternId)
                )
                .anyMatch(pattern ->
                        reqBody.startTime().isBefore(pattern.getEndTime())
                                && pattern.getStartTime()
                                .isBefore(reqBody.endTime())
                );

        if (overlap) {
            throw new BusinessException(
                    RegularShiftErrorCode.PATTERN_TIME_OVERLAP
            );
        }
    }

    private void requireManager(Long workplaceId) {
        Long actorId = rq.getActorId();

        WorkplaceMember actorMember =
                workplaceMemberRepository
                        .findByWorkplaceIdAndUserId(workplaceId, actorId)
                .orElseThrow(
                        () -> new BusinessException(
                                RegularShiftErrorCode.MEMBER_NOT_IN_WORKPLACE
                        )
                );

        if (actorMember.getLeftAt() != null) {
            throw new BusinessException(
                    RegularShiftErrorCode.MEMBER_NOT_ACTIVE
            );
        }

        if (actorMember.getRole() != WorkplaceRole.MANAGER) {
            throw new BusinessException(
                    RegularShiftErrorCode.MANAGER_REQUIRED
            );
        }
    }
}
