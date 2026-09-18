package com.merge.backend.domain.shift.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.merge.backend.domain.shift.dto.ManagerScheduleResponse;
import com.merge.backend.domain.shift.dto.ScheduleShiftResponse;
import com.merge.backend.domain.shift.entity.Schedule;
import com.merge.backend.domain.shift.entity.ScheduleStatus;
import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.entity.ShiftStatus;
import com.merge.backend.domain.shift.exception.ScheduleErrorCode;
import com.merge.backend.domain.shift.repository.RegularShiftPatternRepository;
import com.merge.backend.domain.shift.repository.ScheduleRepository;
import com.merge.backend.domain.shift.repository.ShiftRepository;
import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.workplace.entity.Workplace;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;
import com.merge.backend.global.exception.BusinessException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private RegularShiftPatternRepository regularShiftPatternRepository;

    @Mock
    private ShiftRepository shiftRepository;

    @Mock
    private ShiftService shiftService;

    @InjectMocks
    private ScheduleService scheduleService;

    @Test
    void 주_시작일이_월요일이_아니면_예외가_발생한다() {
        // given
        Long actorUserId = 1L;
        Long workplaceId = 1L;
        LocalDate weekStartDate =
            LocalDate.of(2026, 9, 22); // 화요일

        // when
        BusinessException exception =
            assertThrows(
                BusinessException.class,
                () -> scheduleService.createDraftSchedule(
                    actorUserId,
                    workplaceId,
                    weekStartDate
                )
            );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(
                ScheduleErrorCode.INVALID_WEEK_START_DATE
            );

        verifyNoInteractions(scheduleRepository);
    }

    @Test
    void 같은_주차의_스케줄이_이미_존재하면_예외가_발생한다() {
        // given
        Long actorUserId = 1L;
        Long workplaceId = 1L;
        LocalDate weekStartDate =
            LocalDate.of(2026, 9, 21); // 월요일

        when(
            scheduleRepository
                .existsByWorkplace_IdAndWeekStartDate(
                    workplaceId,
                    weekStartDate
                )
        ).thenReturn(true);

        // when
        BusinessException exception =
            assertThrows(
                BusinessException.class,
                () -> scheduleService.createDraftSchedule(
                    actorUserId,
                    workplaceId,
                    weekStartDate
                )
            );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(
                ScheduleErrorCode.SCHEDULE_ALREADY_EXISTS
            );

        verify(scheduleRepository)
            .existsByWorkplace_IdAndWeekStartDate(
                workplaceId,
                weekStartDate
            );

    }

    @Test
    @DisplayName("SCH-05 - Schedule이 없으면 null을 반환한다")
    void getWeeklyScheduleReturnsNullWhenScheduleDoesNotExist() {

        // given
        Long actorUserId = 1L;
        Long workplaceId = 10L;
        LocalDate weekStartDate =
            LocalDate.of(2026, 9, 21);

        when(
            scheduleRepository
                .findByWorkplace_IdAndWeekStartDate(
                    workplaceId,
                    weekStartDate
                )
        ).thenReturn(Optional.empty());

        // when
        ManagerScheduleResponse result =
            scheduleService.getWeeklySchedule(
                actorUserId,
                workplaceId,
                weekStartDate
            );

        // then
        assertThat(result).isNull();

        verify(scheduleRepository)
            .findByWorkplace_IdAndWeekStartDate(
                workplaceId,
                weekStartDate
            );

        verifyNoInteractions(shiftRepository);
    }

    @Test
    @DisplayName("SCH-05 - Schedule은 존재하지만 Shift가 없으면 빈 shifts를 반환한다")
    void getWeeklyScheduleReturnsEmptyShiftsWhenScheduleExistsWithoutShift() {

        // given
        Long actorUserId = 1L;
        Long workplaceId = 10L;
        Long scheduleId = 20L;

        LocalDate weekStartDate =
            LocalDate.of(2026, 9, 21);

        Schedule schedule =
            mock(Schedule.class);

        Workplace workplace =
            mock(Workplace.class);

        when(schedule.getId())
            .thenReturn(scheduleId);

        when(schedule.getWorkplace())
            .thenReturn(workplace);

        when(workplace.getId())
            .thenReturn(workplaceId);

        when(schedule.getWeekStartDate())
            .thenReturn(weekStartDate);

        when(schedule.getStatus())
            .thenReturn(ScheduleStatus.DRAFT);

        when(
            scheduleRepository
                .findByWorkplace_IdAndWeekStartDate(
                    workplaceId,
                    weekStartDate
                )
        ).thenReturn(
            Optional.of(schedule)
        );

        when(
            shiftRepository
                .findBySchedule_IdOrderByStartAtAscIdAsc(
                    scheduleId
                )
        ).thenReturn(
            List.of()
        );

        // when
        ManagerScheduleResponse result =
            scheduleService.getWeeklySchedule(
                actorUserId,
                workplaceId,
                weekStartDate
            );

        // then
        assertThat(result).isNotNull();

        assertThat(result.scheduleId())
            .isEqualTo(scheduleId);

        assertThat(result.workplaceId())
            .isEqualTo(workplaceId);

        assertThat(result.weekStartDate())
            .isEqualTo(weekStartDate);

        assertThat(result.status())
            .isEqualTo(ScheduleStatus.DRAFT);

        assertThat(result.publishedAt())
            .isNull();

        assertThat(result.shifts())
            .isEmpty();

        verify(scheduleRepository)
            .findByWorkplace_IdAndWeekStartDate(
                workplaceId,
                weekStartDate
            );

        verify(shiftRepository)
            .findBySchedule_IdOrderByStartAtAscIdAsc(
                scheduleId
            );
    }

    @Test
    @DisplayName("SCH-05 - Schedule에 Shift가 있으면 Shift 정보를 DTO로 반환한다")
    void getWeeklyScheduleReturnsShiftResponsesWhenShiftsExist() {

        // given
        Long actorUserId = 1L;
        Long workplaceId = 10L;
        Long scheduleId = 20L;

        Long shiftId = 101L;
        Long memberId = 30L;

        LocalDate weekStartDate =
            LocalDate.of(2026, 9, 21);

        LocalDateTime startAt =
            LocalDateTime.of(
                2026, 9, 21,
                9, 0
            );

        LocalDateTime endAt =
            LocalDateTime.of(
                2026, 9, 21,
                14, 0
            );

        Schedule schedule =
            mock(Schedule.class);

        Workplace workplace =
            mock(Workplace.class);

        Shift shift =
            mock(Shift.class);

        WorkplaceMember member =
            mock(WorkplaceMember.class);

        User user =
            mock(User.class);

        when(schedule.getId())
            .thenReturn(scheduleId);

        when(schedule.getWorkplace())
            .thenReturn(workplace);

        when(workplace.getId())
            .thenReturn(workplaceId);

        when(schedule.getWeekStartDate())
            .thenReturn(weekStartDate);

        when(schedule.getStatus())
            .thenReturn(ScheduleStatus.DRAFT);

        when(shift.getId())
            .thenReturn(shiftId);

        when(shift.getMember())
            .thenReturn(member);

        when(member.getId())
            .thenReturn(memberId);

        when(member.getUser())
            .thenReturn(user);

        when(user.getName())
            .thenReturn("김민수");

        when(member.getRole())
            .thenReturn(WorkplaceRole.EMPLOYEE);

        when(shift.getStartAt())
            .thenReturn(startAt);

        when(shift.getEndAt())
            .thenReturn(endAt);

        when(shift.getStatus())
            .thenReturn(ShiftStatus.SCHEDULED);

        when(
            scheduleRepository
                .findByWorkplace_IdAndWeekStartDate(
                    workplaceId,
                    weekStartDate
                )
        ).thenReturn(
            Optional.of(schedule)
        );

        when(
            shiftRepository
                .findBySchedule_IdOrderByStartAtAscIdAsc(
                    scheduleId
                )
        ).thenReturn(
            List.of(shift)
        );

        // when
        ManagerScheduleResponse result =
            scheduleService.getWeeklySchedule(
                actorUserId,
                workplaceId,
                weekStartDate
            );

        // then
        assertThat(result).isNotNull();

        assertThat(result.shifts())
            .hasSize(1);

        ScheduleShiftResponse shiftResponse =
            result.shifts().get(0);

        assertThat(shiftResponse.shiftId())
            .isEqualTo(shiftId);

        assertThat(shiftResponse.memberId())
            .isEqualTo(memberId);

        assertThat(shiftResponse.memberName())
            .isEqualTo("김민수");

        assertThat(shiftResponse.role())
            .isEqualTo(WorkplaceRole.EMPLOYEE);

        assertThat(shiftResponse.startAt())
            .isEqualTo(startAt);

        assertThat(shiftResponse.endAt())
            .isEqualTo(endAt);

        assertThat(shiftResponse.status())
            .isEqualTo(ShiftStatus.SCHEDULED);

        verify(scheduleRepository)
            .findByWorkplace_IdAndWeekStartDate(
                workplaceId,
                weekStartDate
            );

        verify(shiftRepository)
            .findBySchedule_IdOrderByStartAtAscIdAsc(
                scheduleId
            );
    }

    @Test
    @DisplayName("SCH-05 - weekStartDate가 월요일이 아니면 예외가 발생한다")
    void getWeeklyScheduleThrowsExceptionWhenWeekStartDateIsNotMonday() {

        // given
        Long actorUserId = 1L;
        Long workplaceId = 10L;

        LocalDate weekStartDate =
            LocalDate.of(2026, 9, 22); // 화요일

        // when
        BusinessException exception =
            assertThrows(
                BusinessException.class,
                () -> scheduleService.getWeeklySchedule(
                    actorUserId,
                    workplaceId,
                    weekStartDate
                )
            );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(
                ScheduleErrorCode.INVALID_WEEK_START_DATE
            );

        verifyNoInteractions(
            scheduleRepository,
            shiftRepository
        );
    }

}