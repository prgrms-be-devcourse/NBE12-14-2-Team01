package com.merge.backend.domain.shift.service;

import com.merge.backend.domain.shift.dto.RegularShiftPatternListResponse;
import com.merge.backend.domain.shift.dto.RegularShiftPatternReqBody;
import com.merge.backend.domain.shift.dto.RegularShiftPatternResponse;
import com.merge.backend.domain.shift.entity.RegularShiftPattern;
import com.merge.backend.domain.shift.repository.RegularShiftPatternRepository;
import com.merge.backend.domain.shift.repository.WorkplaceMemberRepository;
import com.merge.backend.domain.shift.repository.WorkplaceRepository;
import com.merge.backend.domain.workplace.entity.Workplace;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
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
        Workplace workplace = workplaceRepository.findById(workplaceId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 workplace 입니다."));

        //member 조회
        WorkplaceMember workplaceMember = workplaceMemberRepository.findById(reqBody.memberId())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 user입니다."));

        //다른 workplace의 member인지 확인
        if(!workplaceMember.getWorkplace().getId().equals(workplaceId)) {
            throw new IllegalArgumentException("올바른 소속이 아닙니다.");
        }

        //현재 member인지 확인
        if(workplaceMember.getLeftAt() != null) {
            throw new IllegalArgumentException("현재 workplace의 구성원이 아닙니다.");
        }

        //끝시간 확인
        if(!reqBody.startTime().isBefore(reqBody.endTime())) {
            throw new RuntimeException("시작 시간은 끝나는 시간보다 빨라야합니다");
        }


        List<RegularShiftPattern> patterns = regularShiftPatternRepository.findByMemberIdAndDayOfWeek(reqBody.memberId(), reqBody.dayOfWeek());

        for(RegularShiftPattern pattern : patterns) {
            if(reqBody.startTime().isBefore(pattern.getEndTime()) &&
            pattern.getStartTime().isBefore(reqBody.endTime())) {
                throw new RuntimeException("기존 정기 근무 시간과 겹칩니다.");
            }
        }

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
        Workplace workplace = workplaceRepository.findById(workplaceId).orElseThrow(
                () -> new RuntimeException("존재하지 않는 근무지입니다.")
        );

        WorkplaceMember member = workplaceMemberRepository.findById(reqBody.memberId()).orElseThrow(
                () -> new RuntimeException("존재하지 않는 근로자입니다.")
        );

        RegularShiftPattern pattern = regularShiftPatternRepository.findById(patternId).orElseThrow(
                () -> new RuntimeException("존재하지 않는 RegularPattern입니다.")
        );

        // 이 pattern이 해당 workplace 소속인지 확인
        if(!pattern
                .getMember()
                .getWorkplace()
                .getId()
                .equals(workplaceId)) {
            throw new RuntimeException("해당 workplace의 정기 근무가 아닙니다");
        }

        if(!member.getWorkplace().getId().equals(workplaceId)) {
            throw new IllegalArgumentException("해당 workplace의 구성원이 아닙니다");
        }

        if(member.getLeftAt()!=null) {
            throw new IllegalArgumentException("현재 근무중이 아닙니다");
        }

        if(!reqBody.startTime().isBefore(reqBody.endTime())) {
            throw new IllegalArgumentException("시작 시간은 종료 시간보다 빨라야 합니다");
        }

        List<RegularShiftPattern> patterns = regularShiftPatternRepository.findByMemberIdAndDayOfWeek(
                reqBody.memberId(),
                reqBody.dayOfWeek()
        );

        for(RegularShiftPattern otherPattern : patterns) {
            if(otherPattern.getId().equals(patternId)) {
                continue;
            }

            if(reqBody.startTime().isBefore(otherPattern.getEndTime())
            && otherPattern.getStartTime().isBefore(reqBody.endTime())) {
                throw new IllegalArgumentException("기존 정기 근무 시간과 겹칩니다.");
            }
        }

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

        Workplace workplace = workplaceRepository.findById(workplaceId).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 근무지입니다.")
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
                () -> new IllegalArgumentException("존재하지 않는 근무지 입니다.")
        );

        RegularShiftPattern pattern = regularShiftPatternRepository.findById(patternId).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 정기 근무입니다.")
        );

        if(!pattern.getMember().getWorkplace().getId().equals(workplaceId)) {
            throw new IllegalArgumentException("해당 workplace의 정기근무가 아닙니다.");
        }

        regularShiftPatternRepository.delete(pattern);

    }
}
