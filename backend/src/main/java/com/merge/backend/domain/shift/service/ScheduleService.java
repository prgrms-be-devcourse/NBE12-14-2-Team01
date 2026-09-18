package com.merge.backend.domain.shift.service;

import com.merge.backend.domain.shift.dto.ManagerScheduleResponse;
import com.merge.backend.domain.shift.entity.Schedule;
import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.exception.ScheduleErrorCode;
import com.merge.backend.domain.shift.repository.RegularShiftPatternRepository;
import com.merge.backend.domain.shift.repository.ScheduleRepository;
import com.merge.backend.domain.shift.repository.ShiftRepository;
import com.merge.backend.global.exception.BusinessException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor

public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final RegularShiftPatternRepository
        regularShiftPatternRepository;
    private final ShiftRepository shiftRepository;
    private final ShiftService shiftService;
    // private final WorkplaceAccessService workplaceAccessService;

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
            new Schedule(
                manager.getWorkplace(),
                weekStartDate
            );

        // 5. Schedule 저장
         Schedule savedSchedule =
            scheduleRepository.save(schedule);
         */

        /*
        // 6. 해당 Workplace에 적용할 Pattern 목록 조회
        List<RegularShiftPattern> patterns =
            regularShiftPatternRepository
                .findByMemberWorkplaceIdAndMemberLeftAtIsNull(
                    workplaceId
                );

        // 7. Pattern → 실제 Shift
        for (RegularShiftPattern pattern : patterns) {

//            LocalDate shiftDate =
//                schedule.resolveDate(
//                    pattern.getDayOfWeek()
//                );
//            LocalDateTime startAt =
//                shiftDate.atTime(
//                    pattern.getStartTime()
//                );
//
//            LocalDateTime endAt =
//                shiftDate.atTime(
//                    pattern.getEndTime()
//                );

//            shiftService.createFromValidatedPattern(
//              savedSchedule,
//              pattern.getMember(),
//              startAt,
//              endAt
//            );

//        }

        ⑧Persistence
        Schedule + Shift N개 저장

        ⑨ 하나라도 실패
        → @Transactional 전체 rollback

        ⑩ 성공
        → 생성된 DRAFT Schedule 반환
        return schedule;
         */

        return null; // savedSchedule;
    }

    @Transactional(readOnly = true)
    public ManagerScheduleResponse getWeeklySchedule(
        Long actorUserId,
        Long workplaceId,
        LocalDate weekStartDate
    ) {

        /*
        workplaceAccessService.requireManager(
            actorUserId,
            workplaceId
        );
        */

        validateWeekStartDate(
            weekStartDate
        );

        Optional<Schedule> optionalSchedule =
            scheduleRepository
                .findByWorkplace_IdAndWeekStartDate(
                    workplaceId,
                    weekStartDate
                );

        if (optionalSchedule.isEmpty()) {
            return null;
        }

        Schedule schedule =
            optionalSchedule.get();

        List<Shift> shifts =
            shiftRepository
                .findBySchedule_IdOrderByStartAtAscIdAsc(
                    schedule.getId()
                );

        return ManagerScheduleResponse.from(
            schedule,
            shifts
        );
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
