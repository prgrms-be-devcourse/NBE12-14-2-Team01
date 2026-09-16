package com.merge.backend.domain.shift.service;

import com.merge.backend.domain.shift.dto.RegularShiftPatternListResponse;
import com.merge.backend.domain.shift.dto.RegularShiftPatternReqBody;
import com.merge.backend.domain.shift.dto.RegularShiftPatternResponse;
import com.merge.backend.domain.shift.entity.RegularShiftPattern;
import com.merge.backend.domain.shift.exception.ShiftErrorCode;
import com.merge.backend.domain.shift.repository.RegularShiftPatternRepository;
import com.merge.backend.domain.shift.repository.WorkplaceMemberRepository;
import com.merge.backend.domain.shift.repository.WorkplaceRepository;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.global.exception.BusinessException;
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

    public RegularShiftPatternResponse register(Long workplaceId,
                         RegularShiftPatternReqBody reqBody)
    {
        //workplace 존재 확인
        validateWorkplaceExists(workplaceId);

        //member 조회
        WorkplaceMember workplaceMember = getActiveMember(workplaceId, reqBody.memberId());

        //끝시간 확인
        validateTime(reqBody);

        validateOverlap(reqBody, null);

        RegularShiftPattern pattern = new RegularShiftPattern(
                workplaceMember,
                reqBody.dayOfWeek(),
                reqBody.startTime(),
                reqBody.endTime()
        );

        RegularShiftPattern savedPattern =  regularShiftPatternRepository.save(pattern);

        return new RegularShiftPatternResponse(
                savedPattern.getId(),
                workplaceMember.getId(),
                workplaceMember.getUser().getName(),
                savedPattern.getDayOfWeek(),
                savedPattern.getStartTime(),
                savedPattern.getEndTime()
        );

    }

    public RegularShiftPatternResponse update(Long workplaceId, Long patternId, RegularShiftPatternReqBody reqBody) {
        validateWorkplaceExists(workplaceId);

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

        workplaceRepository.findById(workplaceId).orElseThrow(
                () -> new BusinessException(
                        ShiftErrorCode.WORKPLACE_NOT_FOUND
                )
        );

        List<RegularShiftPattern> patterns = regularShiftPatternRepository.findByMemberWorkplaceIdAndMemberLeftAtIsNull(workplaceId);

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

        workplaceRepository.findById(workplaceId).orElseThrow(
                () -> new BusinessException(
                        ShiftErrorCode.WORKPLACE_NOT_FOUND
                )
        );

        RegularShiftPattern pattern = regularShiftPatternRepository.findById(patternId).orElseThrow(
                () -> new BusinessException(
                        ShiftErrorCode.PATTERN_NOT_FOUND
                )
        );

        if(!pattern.getMember().getWorkplace().getId().equals(workplaceId)) {
            throw new BusinessException(
                    ShiftErrorCode.PATTERN_NOT_IN_WORKPLACE
            );
        }

        regularShiftPatternRepository.delete(pattern);

    }

    private void validateWorkplaceExists(Long workplaceId) {
        if (!workplaceRepository.existsById(workplaceId)) {
            throw new BusinessException(
                    ShiftErrorCode.WORKPLACE_NOT_FOUND
            );
        }
    }

    private WorkplaceMember getActiveMember(Long workplaceId, Long memberId) {
        WorkplaceMember member =
                workplaceMemberRepository.findById(memberId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        ShiftErrorCode.MEMBER_NOT_FOUND
                                )
                        );

        if (!member.getWorkplace().getId().equals(workplaceId)) {
            throw new BusinessException(
                    ShiftErrorCode.MEMBER_NOT_IN_WORKPLACE
            );
        }

        if (member.getLeftAt() != null) {
            throw new BusinessException(
                    ShiftErrorCode.MEMBER_NOT_ACTIVE
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
                                        ShiftErrorCode.PATTERN_NOT_FOUND
                                )
                        );

        if (!pattern.getMember()
                .getWorkplace()
                .getId()
                .equals(workplaceId)) {

            throw new BusinessException(
                    ShiftErrorCode.PATTERN_NOT_IN_WORKPLACE
            );
        }

        return pattern;
    }

    private void validateTime(RegularShiftPatternReqBody reqBody) {
        if (!reqBody.startTime().isBefore(reqBody.endTime())) {
            throw new BusinessException(
                    ShiftErrorCode.INVALID_PATTERN_TIME
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
                    ShiftErrorCode.PATTERN_TIME_OVERLAP
            );
        }
    }
}
