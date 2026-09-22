package com.merge.backend.domain.shift.service;

import com.merge.backend.domain.shift.dto.ManagerScheduleResponse;
import com.merge.backend.domain.shift.dto.ScheduleCreateResponse;
import com.merge.backend.domain.shift.dto.SchedulePublishResponse;
import com.merge.backend.domain.shift.entity.RegularShiftPattern;
import com.merge.backend.domain.shift.entity.Schedule;
import com.merge.backend.domain.shift.entity.ScheduleStatus;
import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.exception.ScheduleErrorCode;
import com.merge.backend.domain.shift.exception.ShiftErrorCode;
import com.merge.backend.domain.shift.repository.RegularShiftPatternRepository;
import com.merge.backend.domain.shift.repository.ScheduleRepository;
import com.merge.backend.domain.shift.repository.ShiftRepository;
import com.merge.backend.domain.shift.repository.UnavailableTimeRepository;
import com.merge.backend.domain.user.repository.UserRepository;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.service.WorkplaceMemberService;
import com.merge.backend.global.exception.BusinessException;
import com.merge.backend.global.util.TimeRangeUtils;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final UserRepository userRepository;
    private final ShiftService shiftService;
    private final WorkplaceMemberService workplaceMemberService;
    private final UnavailableTimeRepository unavailableTimeRepository;

    private final Clock clock;

    @Transactional
    public ScheduleCreateResponse createDraftSchedule(
        Long actorUserId,
        Long workplaceId,
        LocalDate weekStartDate
    ) {

        // 1. 관리자 확인
        WorkplaceMember manager =
            workplaceMemberService.requireManager(
                actorUserId,
                workplaceId
            );

        // 2. 월요일 검증
        validateWeekStartDate(weekStartDate);

        // 3. 중복 Schedule 검증
        validateScheduleNotExists(
            workplaceId,
            weekStartDate
        );

        // 4. DRAFT Schedule 생성
        Schedule schedule =
            new Schedule(
                manager.getWorkplace(),
                weekStartDate
            );

        // 5. Schedule 저장
        Schedule savedSchedule =
            scheduleRepository.save(schedule);

        // 6. 해당 Workplace에 적용할 Pattern 목록 조회
        List<RegularShiftPattern> patterns =
            regularShiftPatternRepository
                .findByMemberWorkplaceIdAndMemberLeftAtIsNull(
                    workplaceId
                );

        // 7. Pattern → 실제 Shift
        for (RegularShiftPattern pattern : patterns) {

            LocalDate shiftDate =
                savedSchedule.resolveDate(
                    pattern.getDayOfWeek()
                );
            LocalDateTime startAt =
                shiftDate.atTime(
                    pattern.getStartTime()
                );

            LocalDateTime endAt =
                shiftDate.atTime(
                    pattern.getEndTime()
                );

            shiftService.createFromValidatedPattern(
                savedSchedule,
                pattern.getMember(),
                startAt,
                endAt
            );

        }

        /*
        ⑧ Persistence
        Schedule + Shift N개 저장

        ⑨ 하나라도 실패
        → @Transactional 전체 rollback

        ⑩ 성공
        → 생성 결과 DTO 반환
        */

        return ScheduleCreateResponse.from(
            savedSchedule,
            patterns.size()
        );
    }

    @Transactional(readOnly = true)
    public ManagerScheduleResponse getWeeklySchedule(
        Long actorUserId,
        Long workplaceId,
        LocalDate weekStartDate
    ) {

        workplaceMemberService.requireManager(
            actorUserId,
            workplaceId
        );

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

    @Transactional
    public SchedulePublishResponse publishSchedule(
        Long actorUserId,
        Long workplaceId,
        Long scheduleId,
        boolean confirmUnavailableConflict
    ) {
        Schedule schedule =
            getPublishTargetSchedule(
                actorUserId,
                workplaceId,
                scheduleId
            );

        validateDraftSchedule(
            schedule
        );

        List<Shift> shifts =
            getPublishTargetShifts(
                schedule
            );

        validatePublishScheduleBeforeUserLock(
            schedule,
            shifts,
            workplaceId
        );

        lockShiftUsers(
            shifts
        );

        validatePublishScheduleAfterUserLock(
            shifts,
            confirmUnavailableConflict
        );

        publishScheduleState(
            schedule
        );

        return SchedulePublishResponse.from(
            schedule
        );
    }

    private void validateWeekStartDate(
        LocalDate weekStartDate
    ) {
        if (weekStartDate.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new BusinessException(
                ScheduleErrorCode.INVALID_WEEK_START_DATE
            );
        }
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

    private Schedule getScheduleForUpdateOrThrow(
        Long scheduleId
    ) {
        return scheduleRepository
            .findByIdForUpdate(scheduleId)
            .orElseThrow(() -> new BusinessException(
                ScheduleErrorCode.SCHEDULE_NOT_FOUND
            ));
    }

    private void validateScheduleWorkplace(
        Schedule schedule,
        Long workplaceId
    ) {
        if (!schedule.getWorkplace()
            .getId()
            .equals(workplaceId)) {

            throw new BusinessException(
                ScheduleErrorCode.SCHEDULE_NOT_FOUND
            );
        }
    }

    private Schedule getPublishTargetSchedule(
        Long actorUserId,
        Long workplaceId,
        Long scheduleId
    ) {
        workplaceMemberService.requireManager(
            actorUserId,
            workplaceId
        );

        Schedule schedule =
            getScheduleForUpdateOrThrow(scheduleId);

        validateScheduleWorkplace(
            schedule,
            workplaceId
        );

        return schedule;
    }

    private void validateDraftSchedule(
        Schedule schedule
    ) {
        if (schedule.getStatus()
            != ScheduleStatus.DRAFT) {

            throw new BusinessException(
                ScheduleErrorCode.SCHEDULE_NOT_DRAFT
            );
        }
    }

    private List<Shift> getPublishTargetShifts(
        Schedule schedule
    ) {
        List<Shift> shifts =
            shiftRepository
                .findBySchedule_IdOrderByStartAtAscIdAsc(
                    schedule.getId()
                );

        if (shifts.isEmpty()) {
            throw new BusinessException(
                ScheduleErrorCode.EMPTY_SCHEDULE
            );
        }

        return shifts;
    }

    private void validateCurrentShiftMembers(
        List<Shift> shifts,
        Long workplaceId
    ) {
        for (Shift shift : shifts) {
            WorkplaceMember member =
                shift.getMember();

            if (!member.getWorkplace()
                .getId()
                .equals(workplaceId)
                || member.getLeftAt() != null) {

                throw new BusinessException(
                    ScheduleErrorCode.INVALID_SHIFT_MEMBER
                );
            }
        }
    }

    private void validateShiftTimes(
        List<Shift> shifts
    ) {
        for (Shift shift : shifts) {

            if (!TimeRangeUtils.isValidRange(
                shift.getStartAt(),
                shift.getEndAt()
            )) {
                throw new BusinessException(
                    ScheduleErrorCode.INVALID_SHIFT_TIME
                );
            }

            if (!shift.getStartAt()
                .toLocalDate()
                .equals(
                    shift.getEndAt()
                        .toLocalDate()
                )) {

                throw new BusinessException(
                    ScheduleErrorCode.INVALID_SHIFT_SAME_DAY
                );
            }
        }
    }

    private void validateShiftWeekRange(
        Schedule schedule,
        List<Shift> shifts
    ) {
        LocalDateTime scheduleStart =
            schedule.getWeekStartDate()
                .atStartOfDay();

        LocalDateTime scheduleEnd =
            schedule.getWeekStartDate()
                .plusWeeks(1)
                .atStartOfDay();

        for (Shift shift : shifts) {
            if (shift.getStartAt()
                .isBefore(scheduleStart)
                || shift.getEndAt()
                .isAfter(scheduleEnd)) {

                throw new BusinessException(
                    ScheduleErrorCode.INVALID_SHIFT_WEEK
                );
            }
        }
    }

    private void validateInternalShiftOverlap(
        List<Shift> shifts
    ) {
        for (int i = 0; i < shifts.size(); i++) {
            Shift first = shifts.get(i);

            for (int j = i + 1; j < shifts.size(); j++) {
                Shift second = shifts.get(j);

                if (!first.getMember()
                    .getId()
                    .equals(
                        second.getMember()
                            .getId()
                    )) {
                    continue;
                }

                boolean overlaps =
                    first.getStartAt()
                        .isBefore(second.getEndAt())
                        && second.getStartAt()
                        .isBefore(first.getEndAt());

                if (overlaps) {
                    throw new BusinessException(
                        ScheduleErrorCode.SHIFT_OVERLAP
                    );
                }
            }
        }
    }

    private void validateOfficialShiftConflicts(
        List<Shift> shifts
    ) {
        for (Shift shift : shifts) {
            Long userId =
                shift.getMember()
                    .getUser()
                    .getId();

            boolean hasConflict =
                shiftRepository
                    .existsOverlappingOfficialShift(
                        userId,
                        ScheduleStatus.PUBLISHED,
                        shift.getStartAt(),
                        shift.getEndAt(),
                        null
                    );

            if (hasConflict) {
                throw new BusinessException(
                    ScheduleErrorCode.OFFICIAL_SHIFT_CONFLICT
                );
            }
        }
    }

    private boolean hasUnavailableConflict(
        List<Shift> shifts
    ) {
        for (Shift shift : shifts) {
            Long userId =
                shift.getMember()
                    .getUser()
                    .getId();

            boolean hasConflict =
                unavailableTimeRepository
                    .existsOverlappingUnavailableTime(
                        userId,
                        shift.getStartAt(),
                        shift.getEndAt()
                    );

            if (hasConflict) {
                return true;
            }
        }

        return false;
    }

    private void validateUnavailableConflictConfirmation(
        List<Shift> shifts,
        boolean confirmUnavailableConflict
    ) {
        boolean hasConflict =
            hasUnavailableConflict(
                shifts
            );

        if (hasConflict
            && !confirmUnavailableConflict) {

            throw new BusinessException(
                ShiftErrorCode.UNAVAILABLE_TIME_CONFLICT
            );
        }
    }

    private void validatePublishScheduleBeforeUserLock(
        Schedule schedule,
        List<Shift> shifts,
        Long workplaceId
    ) {
        validateCurrentShiftMembers(
            shifts,
            workplaceId
        );

        validateShiftTimes(
            shifts
        );

        validateShiftWeekRange(
            schedule,
            shifts
        );

        validateInternalShiftOverlap(
            shifts
        );
    }

    private void validatePublishScheduleAfterUserLock(
        List<Shift> shifts,
        boolean confirmUnavailableConflict
    ) {
        validateOfficialShiftConflicts(
            shifts
        );

        validateUnavailableConflictConfirmation(
            shifts,
            confirmUnavailableConflict
        );
    }

    private void publishScheduleState(
        Schedule schedule
    ) {
        LocalDateTime publishedAt =
            LocalDateTime.now(clock);

        schedule.publish(
            publishedAt
        );
    }

    private void lockShiftUsers(
        List<Shift> shifts
    ) {
        List<Long> userIds =
            shifts.stream()
                .map(
                    shift ->
                        shift.getMember()
                            .getUser()
                            .getId()
                )
                .distinct()
                .sorted()
                .toList();

        for (Long userId : userIds) {
            userRepository
                .findByIdForUpdate(userId)
                .orElseThrow(
                    () -> new IllegalStateException(
                        "Shift 담당 User를 찾을 수 없습니다."
                    )
                );
        }
    }

}
