package com.merge.backend.domain.shift.service;

import static com.merge.backend.domain.shift.entity.ShiftStatus.SCHEDULED;

import com.merge.backend.domain.shift.dto.ShiftRequest;
import com.merge.backend.domain.shift.entity.Schedule;
import com.merge.backend.domain.shift.entity.ScheduleStatus;
import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.entity.ShiftStatus;
import com.merge.backend.domain.shift.exception.ShiftErrorCode;
import com.merge.backend.domain.shift.repository.ScheduleRepository;
import com.merge.backend.domain.shift.repository.ShiftRepository;
import com.merge.backend.domain.shift.repository.UnavailableTimeRepository;
import com.merge.backend.domain.workplace.entity.Workplace;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.repository.WorkplaceMemberRepository;
import com.merge.backend.domain.workplace.repository.WorkplaceRepository;
import com.merge.backend.domain.workplace.service.WorkplaceMemberService;
import com.merge.backend.global.dto.ConfirmationRequiredResponse;
import com.merge.backend.global.exception.BusinessException;
import com.merge.backend.global.util.TimeRangeUtils;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShiftService {

    private final ScheduleRepository scheduleRepository;
    private final ShiftRepository shiftRepository;
    private final WorkplaceMemberService workplaceMemberService;
    private final WorkplaceMemberRepository workplaceMemberRepository;
    private final WorkplaceRepository workplaceRepository;
    private final UnavailableTimeRepository unavailableTimeRepository;

    @Transactional(readOnly = true)
    public List<Shift> list(
        LocalDate weekStartDate,
        Long currentUserId
    ) {
        validWeekStartDate(weekStartDate);

        return shiftRepository.findAllByWeekStartDateAndCurrentUserId(
            weekStartDate,
            currentUserId
        );
    }

    @Transactional(readOnly = true)
    public Shift detail(Long shiftId, Long currentUserId) {
        Shift shift = shiftRepository.findById(shiftId)
            .orElseThrow(() ->
                new BusinessException(ShiftErrorCode.NOT_FOUND_SHIFT_ERROR));

        //조회할 근무가 PUBLISHED 되지 않았다면
        if (shift.getSchedule().getStatus() != ScheduleStatus.PUBLISHED) {
            throw new BusinessException(ShiftErrorCode.NOT_PUBLISHED_SHIFT);
        }
        //취소된 근무를 조회하려 할 시
        if (shift.getStatus() != ShiftStatus.SCHEDULED) {
            throw new BusinessException(ShiftErrorCode.IS_CANCELED_SHIFT);
        }

        Long shiftUserId = shift.getMember().getUser().getId();

        if (!shiftUserId.equals(currentUserId)) {
            throw new BusinessException(ShiftErrorCode.FORBIDDEN_ACCESS);
        }
        return shift;
    }

    @Transactional
    public Shift create(ShiftRequest reqBody, Long workplaceId, Long scheduleId, Long actorId) {

        //요청 사용자 해당 매장의 MANAGER 권한을 가지고 있는지
        workplaceMemberService.requireManager(actorId, workplaceId);

        //스케줄 관련 예외 검사
        Schedule schedule = validSchedule(scheduleId, workplaceId);

        //근무자 관련 예외 검사
        WorkplaceMember member = validWorkplaceMember(workplaceId, reqBody);

        //다른 근무지의 근무 시간까지의 예외 검증을 위해 userId를 갖고옴
        Long userId = member.getUser().getId();
        //근무 시간 관련 예외 처리
        validShiftTime(reqBody, schedule, member, scheduleId, userId, null);

        //불가능 시간 관련 예외 처리
        validateUnavailableConflictConfirmation(reqBody, userId);

        return saveShift(
            schedule,
            member,
            reqBody.startAt(),
            reqBody.endAt()
        );
    }

    /**
     * SCH-01 전용 Pattern 기반 초기 Shift 생성.
     * <p>
     * ScheduleService에서 관리자 권한과 주차를 검증하고, 현재 구성원의 Pattern을 해당 주차의 날짜·시간으로 변환한 뒤 호출한다. 편집용 충돌 검증은
     * 적용하지 않으며, 공개 시 SCH-06에서 최종 검증한다.
     */
    @Transactional
    public Shift createFromValidatedPattern(
        Schedule schedule,
        WorkplaceMember member,
        LocalDateTime startAt,
        LocalDateTime endAt
    ) {
        return saveShift(
            schedule,
            member,
            startAt,
            endAt
        );
    }

    private Shift saveShift(
        Schedule schedule,
        WorkplaceMember member,
        LocalDateTime startAt,
        LocalDateTime endAt
    ) {
        Shift shift = new Shift(
            schedule,
            member,
            startAt,
            endAt,
            SCHEDULED
        );

        return shiftRepository.save(shift);
    }

    @Transactional
    public Shift modify(
        Long workplaceId,
        Long scheduleId,
        Long shiftId,
        ShiftRequest reqBody,
        Long actorId
    ) {
        //요청 사용자(actorId)가 해당 매장의 MANAGER 권한을 가지고 있는지 검증
        workplaceMemberService.requireManager(actorId, workplaceId);

        //스케줄 관련 예외 검사
        Schedule schedule = validSchedule(scheduleId, workplaceId);

        //근무 관련 예외 검사
        Shift shift = validShift(scheduleId, shiftId);

        //근무자 관련 예외 검사
        WorkplaceMember member = validWorkplaceMember(workplaceId, reqBody);

        //다른 근무지의 근무 시간까지의 예외 검증을 위해 userId를 갖고옴
        Long userId = member.getUser().getId();
        //근무 시간 관련 예외 처리
        validShiftTime(reqBody, schedule, member, scheduleId, userId, shift.getId());

        //불가능 시간 관련 예외 처리
        validateUnavailableConflictConfirmation(reqBody, userId);

        return shift.update(
            member,
            reqBody.startAt(),
            reqBody.endAt()
        );
    }

    @Transactional
    public void delete(Long workplaceId, Long scheduleId, Long shiftId, Long actorId) {

        //요청 사용자(actorId)가 해당 매장의 MANAGER 권한을 가지고 있는지 검증
        workplaceMemberService.requireManager(actorId, workplaceId);

        //근무지가 실제 존재하는지
        Workplace workplace = workplaceRepository.findById(workplaceId)
            .orElseThrow(() ->
                new BusinessException(ShiftErrorCode.NOT_FOUND_WORKPLACE_ERROR)
            );
        //스케줄 관련 예외 검사
        Schedule schedule = validSchedule(scheduleId, workplaceId);
        //근무 관련 예외 검사
        Shift shift = validShift(scheduleId, shiftId);

        shiftRepository.deleteById(shiftId);
    }

    //스케줄 관련 예외 검사 메서드
    private Schedule validSchedule(Long scheduleId, Long workplaceId) {
        //스케줄 존재 확인
        Schedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() ->
                new BusinessException(
                    ShiftErrorCode.NOT_FOUND_SCHEDULE_ERROR)
            );
        //스케줄이 해당 workplace 소속인지 확인
        if (!schedule.getWorkplace().getId().equals(workplaceId)) {
            throw new BusinessException(ShiftErrorCode.INVALID_WORKPLACE_VALUE);
        }
        //스케줄이 DRAFT인지 확인
        if (schedule.getStatus() != ScheduleStatus.DRAFT) {
            throw new BusinessException(ShiftErrorCode.INVALID_STATUS_VALUE);
        }
        return schedule;
    }

    private Shift validShift(Long scheduleId, Long shiftId) {
        //shift 존재 확인
        Shift shift = shiftRepository.findById(shiftId)
            .orElseThrow(() -> new BusinessException(ShiftErrorCode.NOT_FOUND_ERROR)
            );
        //shift가 해당 schedule 소속인지
        if (!shift.getSchedule().getId().equals(scheduleId)) {
            throw new BusinessException(ShiftErrorCode.INVALID_SHIFT_VALUE);
        }
        return shift;
    }

    private WorkplaceMember validWorkplaceMember(Long workplaceId, ShiftRequest reqBody) {
        //WorkplaceMember 존재 확인
        WorkplaceMember member = workplaceMemberRepository.findById(reqBody.memberId())
            .orElseThrow(() ->
                new BusinessException(
                    ShiftErrorCode.NOT_FOUND_ERROR
                )
            );
        //member가 현 workplace의 일원인지 체크 && 퇴사하지 않았는지
        if (!member.getWorkplace().getId().equals(workplaceId) || member.getLeftAt() != null) {
            throw new BusinessException(ShiftErrorCode.INVALID_WORKPLACE_MEMBER_VALUE);
        }
        return member;
    }

    private void validShiftTime(
        ShiftRequest reqBody, Schedule schedule,
        WorkplaceMember member, Long scheduleId,
        Long userId, Long currentShiftId
    ) {
        //p0에서는 자정을 넘어가는 근무는 존재하지 않는다.
        if (!reqBody.startAt().toLocalDate().equals(reqBody.endAt().toLocalDate())) {
            throw new BusinessException(ShiftErrorCode.INVALID_SHIFT_SAME_DAY);
        }
        //startAt < endAt
        if (!TimeRangeUtils.isValidRange(
            reqBody.startAt(),
            reqBody.endAt()
        )) {
            throw new BusinessException(
                ShiftErrorCode.INVALID_BEFORE_AFTER_VALUE
            );
        }
        //Schedule 주차 범위 검증 추가
        validateShiftInScheduleWeek(schedule, reqBody.startAt(), reqBody.endAt());

        //시간 중복(Overlap) 3단계 검증
        validateShiftOverlap(
            scheduleId, member.getId(), userId,
            reqBody.startAt(), reqBody.endAt(), currentShiftId
        );
    }

    // UnavailableTime 충돌에 대한 관리자 확인 여부 검증
    private void validateUnavailableConflictConfirmation(ShiftRequest reqBody, Long userId) {
        // confirmUnavailableConflict가 false일 때만 체크)
        if (!reqBody.confirmUnavailableConflict()) {
            if (unavailableTimeRepository.existsOverlappingUnavailableTime(
                userId, reqBody.startAt(), reqBody.endAt())
            ) {
                // 변경을 적용하지 않고, 관리자 확인이 필요한 Warning을 발생시킨다.
                throw new BusinessException(ShiftErrorCode.UNAVAILABLE_TIME_CONFLICT,
                    new ConfirmationRequiredResponse(true));
            }
        }
    }

    /**
     * Shift의 근무 시간이 Schedule의 주차 범위 내에 들어오는지 검증
     */
    private void validateShiftInScheduleWeek(
        Schedule schedule, LocalDateTime startAt, LocalDateTime endAt
    ) {
        LocalDate weekStartDate = schedule.getWeekStartDate(); // 예: 2026-09-21

        // 주차 범위 계산 [월요일 00:00:00, 다음주 월요일 00:00:00)
        LocalDateTime scheduleStart = weekStartDate.atStartOfDay();
        // 2026-09-21T00:00:00
        LocalDateTime scheduleEnd = weekStartDate.plusWeeks(1).atStartOfDay();
        // 2026-09-28T00:00:00

        // startAt이 scheduleStart보다 앞서거나, endAt이 scheduleEnd보다 뒤에 있으면 예외 발생
        if (startAt.isBefore(scheduleStart) || endAt.isAfter(scheduleEnd)) {
            throw new BusinessException(ShiftErrorCode.INVALID_SHIFT_INSCHEDULE);
        }
    }

    //Shift 시간 중복 (Overlap) 검증 메서드
    private void validateShiftOverlap(
        Long scheduleId,
        Long memberId,
        Long userId,
        java.time.LocalDateTime startAt,
        java.time.LocalDateTime endAt,
        Long currentShiftId
    ) {
        // 1) 동일 Schedule 내 동일 Member 중복 체크
        if (shiftRepository.existsOverlappingInSchedule(
            scheduleId, memberId, startAt, endAt, currentShiftId)
        ) {
            throw new BusinessException(ShiftErrorCode.DUPLICATE_LOCAL_SCHEDULE_TIME);
        }

        // 2) 해당 User의 모든 Workplace 확정 Shift 중복 체크
        if (shiftRepository.existsOverlappingOfficialShift(
            userId, ScheduleStatus.PUBLISHED, startAt, endAt, currentShiftId)
        ) {
            throw new BusinessException(ShiftErrorCode.DUPLICATE_GLOBAL_SCHEDULE_TIME);
        }
    }

    private void validWeekStartDate(LocalDate weekStartDate) {
        if (weekStartDate.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new BusinessException(
                ShiftErrorCode.INVALID_INPUT_VALUE
            );
        }
    }

}
