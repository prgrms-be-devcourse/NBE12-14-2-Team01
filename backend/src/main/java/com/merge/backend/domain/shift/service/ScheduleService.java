package com.merge.backend.domain.shift.service;

import com.merge.backend.domain.shift.entity.Schedule;
import com.merge.backend.domain.shift.exception.ScheduleErrorCode;
import com.merge.backend.domain.shift.repository.ScheduleRepository;
import com.merge.backend.global.exception.BusinessException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor

public class ScheduleService {

    private final ScheduleRepository scheduleRepository;

    @Transactional
    public Schedule createDraftSchedule(
        Long actorUserId,
        Long workplaceId,
        LocalDate weekStartDate
    ) {

        /*
        // 1. 관리자 확인
        WorkplaceMember manager =
            workplaceAccessService.requireManager(
                actorUserId,
                workplaceId
            );
         */

        // 2. 월요일 검증
        validateWeekStartDate(weekStartDate);

        // 3. 중복 Schedule 검증
        validateScheduleNotExists(
            workplaceId,
            weekStartDate
        );

        /*
        // 4. DRAFT Schedule 생성
       Schedule schedule =
            Schedule.createDraft(
                manager.getWorkplace(),
                weekStartDate
            );

        // 5. Schedule 저장
         scheduleRepository.save(schedule);
         */


        /*
        ⑤ Pattern
        해당 Workplace에 적용할 Pattern 목록 조회
        List<RegularShiftPattern> patterns =
            regularShiftPatternService
                .findCurrentPatterns(workplaceId);

        // 7. Pattern → 실제 Shift
        for (RegularShiftPattern pattern : patterns) {
            pattern.getMember();
            pattern.getDayOfWeek();
            pattern.getStartTime();
            pattern.getEndTime();

            ⑥ Schedule orchestration
            Pattern마다:
            dayOfWeek → 실제 LocalDate 계산

            LocalDate + startTime
            → startAt

            LocalDate + endTime
            → endAt

            }



        ⑦ Shift
        각 Pattern에 대해
        Schedule + member + startAt + endAt
        로 유효한 Shift 생성
        Shift shift =
            shiftService.createScheduledShift(
                schedule,
                pattern.getMember(),
                startAt,
                endAt
            );

        ⑧ Persistence
        Schedule + Shift N개 저장

        ⑨ 하나라도 실패
        → @Transactional 전체 rollback

        ⑩ 성공
        → 생성된 DRAFT Schedule 반환
         */

        return null; // schedule;
    }

    private void validateWeekStartDate(
        LocalDate weekStartDate
    ) {
        if (weekStartDate.getDayOfWeek() != DayOfWeek.MONDAY)
            throw new BusinessException(
                ScheduleErrorCode.INVALID_WEEK_START_DATE
            );
    }

    private void validateScheduleNotExists(
        Long workplaceId,
        LocalDate weekStartDate
    ) {
        boolean exists =
            scheduleRepository
                .existsByWorkplace_IdAndWeekStartDate(
                    workplaceId,
                    weekStartDate
                );

        if (exists) {
            throw new BusinessException(
                ScheduleErrorCode.SCHEDULE_ALREADY_EXISTS
            );
        }
    }

}
