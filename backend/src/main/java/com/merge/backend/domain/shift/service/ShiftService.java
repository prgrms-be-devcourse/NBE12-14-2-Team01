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
import com.merge.backend.domain.workplace.entity.WorkplaceRole;
import com.merge.backend.domain.workplace.repository.WorkplaceMemberRepository;
import com.merge.backend.domain.workplace.repository.WorkplaceRepository;
import com.merge.backend.global.exception.BusinessException;
import com.merge.backend.global.util.TimeRangeUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShiftService {

    private final ScheduleRepository scheduleRepository;
    private final ShiftRepository shiftRepository;
    private final WorkplaceMemberRepository workplaceMemberRepository;
    private final WorkplaceRepository workplaceRepository;
    private final UnavailableTimeRepository unavailableTimeRepository;

    @Transactional(readOnly = true)
    public Shift detail(Long shiftId, Long currentUserId) {
        Shift shift = shiftRepository.findById(shiftId)
            .orElseThrow(() ->
                new BusinessException(ShiftErrorCode.NOT_FOUND_SHIFT_ERROR));

        //조회할 근무가 PUBLISHED 되지 않았다면
        if(shift.getSchedule().getStatus() != ScheduleStatus.PUBLISHED) {
            throw new BusinessException(ShiftErrorCode.NOT_PUBLISHED_SHIFT);
        }
        //취소된 근무를 조회하려 할 시
        if(shift.getStatus() != ShiftStatus.SCHEDULED) {
            throw new BusinessException(ShiftErrorCode.IS_CANCELED_SHIFT);
        }

        Long shiftUserId = shift.getMember().getUser().getId();

        if(!shiftUserId.equals(currentUserId)){
            throw new BusinessException(ShiftErrorCode.FORBIDDEN_ACCESS);
        }
        return shift;
    }
    @Transactional
    public Shift create(ShiftRequest reqBody, Long workplaceId, Long scheduleId, Long actorId) {

        //요청 사용자 해당 매장의 MANAGER 권한을 가지고 있는지
        if(isNotManager(workplaceId, actorId)) {
            throw new BusinessException(ShiftErrorCode.FORBIDDEN_ACCESS);
        }

        //스케줄 존재 확인
        Schedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() ->
                new BusinessException(
                    ShiftErrorCode.NOT_FOUND_SCHEDULE_ERROR
                )
            );
        //스케줄이 해당 workplace 소속인지 확인
        if(!schedule.getWorkplace().getId().equals(workplaceId)) {
            throw new BusinessException(ShiftErrorCode.INVALID_WORKPLACE_VALUE);
        }
        //스케줄이 DRAFT인지 확인
        if(schedule.getStatus() != ScheduleStatus.DRAFT) {
            throw new BusinessException(ShiftErrorCode.INVALID_STATUS_VALUE);
        }
        //WorkplaceMember 존재 확인
        WorkplaceMember member = workplaceMemberRepository.findById(reqBody.memberId())
            .orElseThrow(() ->
                new BusinessException(
                    ShiftErrorCode.NOT_FOUND_ERROR
                )
            );
        //member가 현 workplace의 일원인지 체크 && 퇴사하지 않았는지
        if(!member.getWorkplace().getId().equals(workplaceId) || member.getLeftAt() != null) {
            throw new BusinessException(ShiftErrorCode.INVALID_WORKPLACE_MEMBER_VALUE);
        }
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
        Long userId = member.getUser().getId();
        validateShiftOverlap(
            scheduleId, member.getId(), userId,
            reqBody.startAt(), reqBody.endAt(), null
        );

        // confirmUnavailableConflict가 false일 때만 체크)
        if (!reqBody.confirmUnavailableConflict()) {
            if (unavailableTimeRepository.existsOverlappingUnavailableTime(
                userId, reqBody.startAt(), reqBody.endAt())
            ) {
                // Shift를 생성하지 않고, 관리자 확인 안내 예외/응답을 던짐
                throw new BusinessException(ShiftErrorCode.UNAVAILABLE_TIME_CONFLICT);
            }
        }
        Shift newShift = new Shift(
            schedule,
            member,
            reqBody.startAt(),
            reqBody.endAt(),
            SCHEDULED
        );
        return shiftRepository.save(newShift);
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

    //요청자가 해당 workplaceId의 MANAGER 권한을 보유하고 있는지
    private boolean isNotManager(Long workplaceId, Long actorId) {

        WorkplaceMember requester = workplaceMemberRepository
            .findByWorkplaceIdAndUserId(workplaceId, actorId)
            .orElseThrow(() ->
                new BusinessException(ShiftErrorCode.INVALID_WORKPLACE_MEMBER_VALUE));

        if (requester.getRole().equals(WorkplaceRole.MANAGER)) {
            return false;
        }
        return true;
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
        if(isNotManager(workplaceId, actorId)){
            throw new BusinessException(ShiftErrorCode.FORBIDDEN_ACCESS);
        };

        //스케줄 존재 확인
        Schedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() ->
                new BusinessException(
                    ShiftErrorCode.NOT_FOUND_SCHEDULE_ERROR
                )
            );
        //스케줄이 해당 workplace 소속인지 확인
        if(!schedule.getWorkplace().getId().equals(workplaceId)) {
            throw new BusinessException(ShiftErrorCode.INVALID_WORKPLACE_VALUE);
        }
        //스케줄이 DRAFT인지 확인
        if(schedule.getStatus() != ScheduleStatus.DRAFT) {
            throw new BusinessException(ShiftErrorCode.INVALID_STATUS_VALUE);
        }
        //shift 존재 확인
        Shift shift = shiftRepository.findById(shiftId)
            .orElseThrow(() -> new BusinessException(ShiftErrorCode.NOT_FOUND_ERROR)
            );
        //shift가 해당 schedule 소속인지
        if(!shift.getSchedule().getId().equals(scheduleId)){
            throw new BusinessException(ShiftErrorCode.INVALID_SHIFT_VALUE);
        }

        WorkplaceMember member = workplaceMemberRepository.findById(reqBody.memberId())
            .orElseThrow(() ->
                new BusinessException(ShiftErrorCode.NOT_FOUND_ERROR
                )
            );

        //member가 현 workplace의 일원인지 체크 && 퇴사하지 않았는지
        if(!member.getWorkplace().getId().equals(workplaceId) ||
            member.getLeftAt() != null) {
            throw new BusinessException(ShiftErrorCode.INVALID_WORKPLACE_MEMBER_VALUE);
        }
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
        Long userId = member.getUser().getId();

        //자기 자신은 overlap 검사에서 제외
        validateShiftOverlap(
            scheduleId, member.getId(), userId,
            reqBody.startAt(), reqBody.endAt(), shiftId
        );

        // confirmUnavailableConflict가 false일 때만 체크)
        if (!reqBody.confirmUnavailableConflict()) {
            if (unavailableTimeRepository.existsOverlappingUnavailableTime(
                userId, reqBody.startAt(), reqBody.endAt())
            ) {
                // Shift를 생성하지 않고, 관리자 확인 안내 예외/응답을 던짐
                throw new BusinessException(ShiftErrorCode.UNAVAILABLE_TIME_CONFLICT);
            }
        }
        return shift.update(
            member,
            reqBody.startAt(),
            reqBody.endAt()
        );
    }

    public void delete(Long workplaceId, Long scheduleId, Long shiftId, Long actorId) {

        Workplace workplace = workplaceRepository.findById(workplaceId)
            .orElseThrow(() ->
                new BusinessException(ShiftErrorCode.NOT_FOUND_WORKPLACE_ERROR)
            );
        //나중에 workplace의 매니저 인가 로직으로 변환
        if(isNotManager(workplaceId, actorId)){
            throw new BusinessException(ShiftErrorCode.FORBIDDEN_ACCESS);
        }
        //스케줄 존재 확인
        Schedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() ->
                new BusinessException(
                    ShiftErrorCode.NOT_FOUND_SCHEDULE_ERROR)
            );
        //스케줄이 해당 workplace 소속인지 확인
        if(!schedule.getWorkplace().getId().equals(workplaceId)) {
            throw new BusinessException(ShiftErrorCode.INVALID_WORKPLACE_VALUE);
        }
        //스케줄이 DRAFT인지 확인
        if(schedule.getStatus() != ScheduleStatus.DRAFT) {
            throw new BusinessException(ShiftErrorCode.INVALID_STATUS_VALUE);
        }
        //shift 존재 확인
        Shift shift = shiftRepository.findById(shiftId)
            .orElseThrow(() -> new BusinessException(ShiftErrorCode.NOT_FOUND_ERROR)
            );
        //shift가 해당 schedule 소속인지
        if(!shift.getSchedule().getId().equals(scheduleId)){
            throw new BusinessException(ShiftErrorCode.INVALID_SHIFT_VALUE);
        }
        shiftRepository.deleteById(shiftId);
    }
}
