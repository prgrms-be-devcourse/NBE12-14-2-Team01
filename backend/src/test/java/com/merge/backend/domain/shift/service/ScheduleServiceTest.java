package com.merge.backend.domain.shift.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.merge.backend.domain.shift.dto.ManagerScheduleResponse;
import com.merge.backend.domain.shift.dto.ScheduleCreateResponse;
import com.merge.backend.domain.shift.dto.SchedulePublishResponse;
import com.merge.backend.domain.shift.dto.ScheduleShiftResponse;
import com.merge.backend.domain.shift.entity.RegularShiftPattern;
import com.merge.backend.domain.shift.entity.Schedule;
import com.merge.backend.domain.shift.entity.ScheduleStatus;
import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.entity.ShiftStatus;
import com.merge.backend.domain.shift.exception.ScheduleErrorCode;
import com.merge.backend.domain.shift.exception.ShiftErrorCode;
import com.merge.backend.domain.shift.repository.RegularShiftPatternRepository;
import com.merge.backend.domain.shift.repository.ScheduleRepository;
import com.merge.backend.domain.shift.repository.ShiftRepository;
import com.merge.backend.domain.shift.repository.UnavailableTimeRepository;
import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.workplace.entity.Workplace;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;
import com.merge.backend.domain.workplace.exception.WorkplaceErrorCode;
import com.merge.backend.domain.workplace.service.WorkplaceMemberService;
import com.merge.backend.global.exception.BusinessException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
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

    @Mock
    private WorkplaceMemberService workplaceMemberService;

    @InjectMocks
    private ScheduleService scheduleService;

    @Mock
    private UnavailableTimeRepository unavailableTimeRepository;

    @Mock
    private Clock clock;

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

        verify(workplaceMemberService)
            .requireManager(
                actorUserId,
                workplaceId
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

        verify(workplaceMemberService)
            .requireManager(
                actorUserId,
                workplaceId
            );

        verify(scheduleRepository)
            .existsByWorkplace_IdAndWeekStartDate(
                workplaceId,
                weekStartDate
            );

    }

    @Test
    @DisplayName("SCH-01 - 유효한 요청이면 빈 DRAFT Schedule 생성 결과를 반환한다")
    void createDraftScheduleCreatesAndReturnsEmptyDraftSchedule() {

        // given
        Long actorUserId = 1L;
        Long workplaceId = 10L;

        LocalDate weekStartDate =
            LocalDate.of(2026, 9, 21);

        WorkplaceMember manager =
            mock(WorkplaceMember.class);

        Workplace workplace =
            mock(Workplace.class);

        when(
            workplaceMemberService.requireManager(
                actorUserId,
                workplaceId
            )
        ).thenReturn(manager);

        when(manager.getWorkplace())
            .thenReturn(workplace);

        when(workplace.getId())
            .thenReturn(workplaceId);

        when(
            scheduleRepository
                .existsByWorkplace_IdAndWeekStartDate(
                    workplaceId,
                    weekStartDate
                )
        ).thenReturn(false);

        Schedule savedSchedule =
            new Schedule(
                workplace,
                weekStartDate
            );

        when(scheduleRepository.save(any(Schedule.class)))
            .thenReturn(savedSchedule);

        when(
            regularShiftPatternRepository
                .findByMemberWorkplaceIdAndMemberLeftAtIsNull(
                    workplaceId
                )
        ).thenReturn(
            List.of()
        );

        // when
        ScheduleCreateResponse result =
            scheduleService.createDraftSchedule(
                actorUserId,
                workplaceId,
                weekStartDate
            );

        // then
        assertThat(result).isNotNull();

        assertThat(result.workplaceId())
            .isEqualTo(workplaceId);

        assertThat(result.weekStartDate())
            .isEqualTo(weekStartDate);

        assertThat(result.status())
            .isEqualTo(ScheduleStatus.DRAFT);

        assertThat(result.shiftCount())
            .isZero();

        verifyNoInteractions(shiftService);
    }

    @Test
    @DisplayName("SCH-01 - Pattern을 해당 주차의 실제 Shift 시간으로 변환한다")
    void createDraftScheduleConvertsPatternToShiftDateTime() {

        // given
        Long actorUserId = 1L;
        Long workplaceId = 10L;

        LocalDate weekStartDate =
            LocalDate.of(2026, 9, 21);

        WorkplaceMember manager =
            mock(WorkplaceMember.class);

        Workplace workplace =
            mock(Workplace.class);

        WorkplaceMember patternMember =
            mock(WorkplaceMember.class);

        RegularShiftPattern pattern =
            mock(RegularShiftPattern.class);

        when(
            workplaceMemberService.requireManager(
                actorUserId,
                workplaceId
            )
        ).thenReturn(manager);

        when(manager.getWorkplace())
            .thenReturn(workplace);

        when(workplace.getId())
            .thenReturn(workplaceId);

        when(
            scheduleRepository
                .existsByWorkplace_IdAndWeekStartDate(
                    workplaceId,
                    weekStartDate
                )
        ).thenReturn(false);

        // 저장된 Schedule을 미리 준비
        Schedule savedSchedule =
            new Schedule(
                workplace,
                weekStartDate
            );

        when(scheduleRepository.save(any(Schedule.class)))
            .thenReturn(savedSchedule);

        when(
            regularShiftPatternRepository
                .findByMemberWorkplaceIdAndMemberLeftAtIsNull(
                    workplaceId
                )
        ).thenReturn(
            List.of(pattern)
        );

        when(pattern.getDayOfWeek())
            .thenReturn(DayOfWeek.WEDNESDAY);

        when(pattern.getStartTime())
            .thenReturn(LocalTime.of(9, 0));

        when(pattern.getEndTime())
            .thenReturn(LocalTime.of(14, 0));

        when(pattern.getMember())
            .thenReturn(patternMember);

        // when
        ScheduleCreateResponse result =
            scheduleService.createDraftSchedule(
                actorUserId,
                workplaceId,
                weekStartDate
            );

        // then
        assertThat(result).isNotNull();

        assertThat(result.workplaceId())
            .isEqualTo(workplaceId);

        assertThat(result.weekStartDate())
            .isEqualTo(weekStartDate);

        assertThat(result.status())
            .isEqualTo(ScheduleStatus.DRAFT);

        assertThat(result.shiftCount())
            .isEqualTo(1);

        verify(shiftService)
            .createFromValidatedPattern(
                savedSchedule,
                patternMember,
                LocalDateTime.of(
                    2026, 9, 23,
                    9, 0
                ),
                LocalDateTime.of(
                    2026, 9, 23,
                    14, 0
                )
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
        ).thenReturn(
            Optional.empty()
        );

        // when
        ManagerScheduleResponse result =
            scheduleService.getWeeklySchedule(
                actorUserId,
                workplaceId,
                weekStartDate
            );

        // then
        assertThat(result)
            .isNull();

        verify(workplaceMemberService)
            .requireManager(
                actorUserId,
                workplaceId
            );

        verify(scheduleRepository)
            .findByWorkplace_IdAndWeekStartDate(
                workplaceId,
                weekStartDate
            );

        verifyNoInteractions(
            shiftRepository
        );
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

        verify(workplaceMemberService)
            .requireManager(
                actorUserId,
                workplaceId
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

        verify(workplaceMemberService)
            .requireManager(
                actorUserId,
                workplaceId
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

        verify(workplaceMemberService)
            .requireManager(
                actorUserId,
                workplaceId
            );

        verifyNoInteractions(
            scheduleRepository,
            shiftRepository
        );
    }

    @Test
    @DisplayName("SCH-05 - MANAGER가 아니면 Schedule을 조회하지 않고 예외가 발생한다")
    void getWeeklyScheduleThrowsExceptionWhenActorIsNotManager() {

        // given
        Long actorUserId = 1L;
        Long workplaceId = 10L;

        LocalDate weekStartDate =
            LocalDate.of(2026, 9, 21);

        when(
            workplaceMemberService.requireManager(
                actorUserId,
                workplaceId
            )
        ).thenThrow(
            new BusinessException(
                WorkplaceErrorCode.MANAGER_REQUIRED
            )
        );

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
                WorkplaceErrorCode.MANAGER_REQUIRED
            );

        verify(workplaceMemberService)
            .requireManager(
                actorUserId,
                workplaceId
            );

        verifyNoInteractions(
            scheduleRepository,
            shiftRepository
        );
    }

    @Test
    @DisplayName("SCH-05 - PUBLISHED Schedule도 publishedAt과 함께 조회한다")
    void getWeeklyScheduleReturnsPublishedSchedule() {

        // given
        Long actorUserId = 1L;
        Long workplaceId = 10L;
        Long scheduleId = 20L;

        LocalDate weekStartDate =
            LocalDate.of(2026, 9, 21);

        LocalDateTime publishedAt =
            LocalDateTime.of(
                2026, 9, 19,
                18, 30
            );

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
            .thenReturn(ScheduleStatus.PUBLISHED);

        when(schedule.getPublishedAt())
            .thenReturn(publishedAt);

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
        assertThat(result)
            .isNotNull();

        assertThat(result.status())
            .isEqualTo(
                ScheduleStatus.PUBLISHED
            );

        assertThat(result.publishedAt())
            .isEqualTo(
                publishedAt
            );

        assertThat(result.shifts())
            .isEmpty();

        verify(workplaceMemberService)
            .requireManager(
                actorUserId,
                workplaceId
            );

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
    @DisplayName("SCH-06 - 유효한 DRAFT Schedule이면 공개에 성공한다")
    void publishSchedulePublishesValidDraftSchedule() {

        // given
        Long actorUserId = 1L;
        Long workplaceId = 10L;
        Long scheduleId = 20L;
        Long userId = 30L;

        LocalDate weekStartDate =
            LocalDate.of(
                2026, 9, 21
            );

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

        LocalDateTime publishedAt =
            LocalDateTime.of(
                2026, 9, 19,
                18, 30
            );

        Schedule schedule =
            mock(Schedule.class);

        Shift shift =
            mock(Shift.class);

        Workplace workplace =
            mock(Workplace.class);

        WorkplaceMember member =
            mock(WorkplaceMember.class);

        User user =
            mock(User.class);

        when(
            scheduleRepository.findById(
                scheduleId
            )
        ).thenReturn(
            Optional.of(schedule)
        );

        when(schedule.getId())
            .thenReturn(scheduleId);

        when(schedule.getWorkplace())
            .thenReturn(workplace);

        when(workplace.getId())
            .thenReturn(workplaceId);

        when(schedule.getWeekStartDate())
            .thenReturn(weekStartDate);

        when(schedule.getStatus())
            .thenReturn(
                ScheduleStatus.DRAFT,
                ScheduleStatus.PUBLISHED
            );

        when(schedule.getPublishedAt())
            .thenReturn(publishedAt);

        when(
            shiftRepository
                .findBySchedule_IdOrderByStartAtAscIdAsc(
                    scheduleId
                )
        ).thenReturn(
            List.of(shift)
        );

        when(shift.getMember())
            .thenReturn(member);

        when(member.getWorkplace())
            .thenReturn(workplace);

        when(member.getLeftAt())
            .thenReturn(null);

        when(member.getUser())
            .thenReturn(user);

        when(user.getId())
            .thenReturn(userId);

        when(shift.getStartAt())
            .thenReturn(startAt);

        when(shift.getEndAt())
            .thenReturn(endAt);

        when(clock.instant())
            .thenReturn(
                Instant.parse(
                    "2026-09-19T09:30:00Z"
                )
            );

        when(clock.getZone())
            .thenReturn(
                ZoneId.of("Asia/Seoul")
            );

        // when
        SchedulePublishResponse result =
            scheduleService.publishSchedule(
                actorUserId,
                workplaceId,
                scheduleId,
                false
            );

        // then
        assertThat(result.scheduleId())
            .isEqualTo(scheduleId);

        assertThat(result.workplaceId())
            .isEqualTo(workplaceId);

        assertThat(result.weekStartDate())
            .isEqualTo(weekStartDate);

        assertThat(result.status())
            .isEqualTo(
                ScheduleStatus.PUBLISHED
            );

        assertThat(result.publishedAt())
            .isEqualTo(publishedAt);

        verify(schedule)
            .publish(publishedAt);
    }

    @Test
    @DisplayName("SCH-06 - 이미 PUBLISHED인 Schedule은 다시 공개할 수 없다")
    void publishScheduleThrowsExceptionWhenAlreadyPublished() {

        // given
        Long actorUserId = 1L;
        Long workplaceId = 10L;
        Long scheduleId = 20L;

        Schedule schedule =
            mock(Schedule.class);

        Workplace workplace =
            mock(Workplace.class);

        when(
            scheduleRepository.findById(
                scheduleId
            )
        ).thenReturn(
            Optional.of(schedule)
        );

        when(schedule.getWorkplace())
            .thenReturn(workplace);

        when(workplace.getId())
            .thenReturn(workplaceId);

        when(schedule.getStatus())
            .thenReturn(
                ScheduleStatus.PUBLISHED
            );

        // when
        BusinessException exception =
            assertThrows(
                BusinessException.class,
                () -> scheduleService.publishSchedule(
                    actorUserId,
                    workplaceId,
                    scheduleId,
                    false
                )
            );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(
                ScheduleErrorCode.SCHEDULE_NOT_DRAFT
            );

        verify(workplaceMemberService)
            .requireManager(
                actorUserId,
                workplaceId
            );

        verify(scheduleRepository)
            .findById(
                scheduleId
            );

        verifyNoInteractions(
            shiftRepository,
            unavailableTimeRepository
        );
    }

    @Test
    @DisplayName("SCH-06 - Shift가 없는 DRAFT Schedule은 공개할 수 없다")
    void publishScheduleThrowsExceptionWhenDraftScheduleHasNoShifts() {

        // given
        Long actorUserId = 1L;
        Long workplaceId = 10L;
        Long scheduleId = 20L;

        Schedule schedule =
            mock(Schedule.class);

        Workplace workplace =
            mock(Workplace.class);

        when(
            scheduleRepository.findById(
                scheduleId
            )
        ).thenReturn(
            Optional.of(schedule)
        );

        when(schedule.getWorkplace())
            .thenReturn(workplace);

        when(workplace.getId())
            .thenReturn(workplaceId);

        when(schedule.getStatus())
            .thenReturn(
                ScheduleStatus.DRAFT
            );

        when(schedule.getId())
            .thenReturn(scheduleId);

        when(
            shiftRepository
                .findBySchedule_IdOrderByStartAtAscIdAsc(
                    scheduleId
                )
        ).thenReturn(
            List.of()
        );

        // when
        BusinessException exception =
            assertThrows(
                BusinessException.class,
                () -> scheduleService.publishSchedule(
                    actorUserId,
                    workplaceId,
                    scheduleId,
                    false
                )
            );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(
                ScheduleErrorCode.EMPTY_SCHEDULE
            );

        verify(workplaceMemberService)
            .requireManager(
                actorUserId,
                workplaceId
            );

        verify(scheduleRepository)
            .findById(
                scheduleId
            );

        verify(shiftRepository)
            .findBySchedule_IdOrderByStartAtAscIdAsc(
                scheduleId
            );

        verifyNoInteractions(
            unavailableTimeRepository,
            clock
        );
    }

    @Test
    @DisplayName("SCH-06 - 현재 구성원이 아닌 Shift 담당자가 있으면 공개할 수 없다")
    void publishScheduleThrowsExceptionWhenShiftMemberHasLeft() {

        // given
        Long actorUserId = 1L;
        Long workplaceId = 10L;
        Long scheduleId = 20L;

        Schedule schedule =
            mock(Schedule.class);

        Shift shift =
            mock(Shift.class);

        Workplace workplace =
            mock(Workplace.class);

        WorkplaceMember member =
            mock(WorkplaceMember.class);

        when(
            scheduleRepository.findById(
                scheduleId
            )
        ).thenReturn(
            Optional.of(schedule)
        );

        when(schedule.getWorkplace())
            .thenReturn(workplace);

        when(workplace.getId())
            .thenReturn(workplaceId);

        when(schedule.getStatus())
            .thenReturn(
                ScheduleStatus.DRAFT
            );

        when(schedule.getId())
            .thenReturn(scheduleId);

        when(
            shiftRepository
                .findBySchedule_IdOrderByStartAtAscIdAsc(
                    scheduleId
                )
        ).thenReturn(
            List.of(shift)
        );

        when(shift.getMember())
            .thenReturn(member);

        when(member.getWorkplace())
            .thenReturn(workplace);

        when(member.getLeftAt())
            .thenReturn(
                LocalDateTime.of(
                    2026, 9, 18,
                    10, 0
                )
            );

        // when
        BusinessException exception =
            assertThrows(
                BusinessException.class,
                () -> scheduleService.publishSchedule(
                    actorUserId,
                    workplaceId,
                    scheduleId,
                    false
                )
            );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(
                ScheduleErrorCode.INVALID_SHIFT_MEMBER
            );

        verifyNoInteractions(
            unavailableTimeRepository,
            clock
        );
    }

    @Test
    @DisplayName("SCH-06 - 다른 Workplace의 Shift 담당자가 있으면 공개할 수 없다")
    void publishScheduleThrowsExceptionWhenShiftMemberBelongsToOtherWorkplace() {

        // given
        Long actorUserId = 1L;
        Long workplaceId = 10L;
        Long otherWorkplaceId = 99L;
        Long scheduleId = 20L;

        Schedule schedule =
            mock(Schedule.class);

        Shift shift =
            mock(Shift.class);

        Workplace scheduleWorkplace =
            mock(Workplace.class);

        Workplace otherWorkplace =
            mock(Workplace.class);

        WorkplaceMember member =
            mock(WorkplaceMember.class);

        when(
            scheduleRepository.findById(
                scheduleId
            )
        ).thenReturn(
            Optional.of(schedule)
        );

        when(schedule.getWorkplace())
            .thenReturn(scheduleWorkplace);

        when(scheduleWorkplace.getId())
            .thenReturn(workplaceId);

        when(schedule.getStatus())
            .thenReturn(
                ScheduleStatus.DRAFT
            );

        when(schedule.getId())
            .thenReturn(scheduleId);

        when(
            shiftRepository
                .findBySchedule_IdOrderByStartAtAscIdAsc(
                    scheduleId
                )
        ).thenReturn(
            List.of(shift)
        );

        when(shift.getMember())
            .thenReturn(member);

        when(member.getWorkplace())
            .thenReturn(otherWorkplace);

        when(otherWorkplace.getId())
            .thenReturn(otherWorkplaceId);

        // when
        BusinessException exception =
            assertThrows(
                BusinessException.class,
                () -> scheduleService.publishSchedule(
                    actorUserId,
                    workplaceId,
                    scheduleId,
                    false
                )
            );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(
                ScheduleErrorCode.INVALID_SHIFT_MEMBER
            );

        verifyNoInteractions(
            unavailableTimeRepository,
            clock
        );
    }

    @Test
    @DisplayName("SCH-06 - Shift 시작 시각과 종료 시각이 같으면 공개할 수 없다")
    void publishScheduleThrowsExceptionWhenShiftStartEqualsEnd() {

        // given
        Long actorUserId = 1L;
        Long workplaceId = 10L;
        Long scheduleId = 20L;

        LocalDateTime invalidAt =
            LocalDateTime.of(
                2026, 9, 21,
                10, 0
            );

        Schedule schedule =
            mock(Schedule.class);

        Shift shift =
            mock(Shift.class);

        Workplace workplace =
            mock(Workplace.class);

        WorkplaceMember member =
            mock(WorkplaceMember.class);

        when(
            scheduleRepository.findById(
                scheduleId
            )
        ).thenReturn(
            Optional.of(schedule)
        );

        when(schedule.getWorkplace())
            .thenReturn(workplace);

        when(workplace.getId())
            .thenReturn(workplaceId);

        when(schedule.getStatus())
            .thenReturn(
                ScheduleStatus.DRAFT
            );

        when(schedule.getId())
            .thenReturn(scheduleId);

        when(
            shiftRepository
                .findBySchedule_IdOrderByStartAtAscIdAsc(
                    scheduleId
                )
        ).thenReturn(
            List.of(shift)
        );

        when(shift.getMember())
            .thenReturn(member);

        when(member.getWorkplace())
            .thenReturn(workplace);

        when(member.getLeftAt())
            .thenReturn(null);

        when(shift.getStartAt())
            .thenReturn(invalidAt);

        when(shift.getEndAt())
            .thenReturn(invalidAt);

        // when
        BusinessException exception =
            assertThrows(
                BusinessException.class,
                () -> scheduleService.publishSchedule(
                    actorUserId,
                    workplaceId,
                    scheduleId,
                    false
                )
            );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(
                ScheduleErrorCode.INVALID_SHIFT_TIME
            );

        verifyNoInteractions(
            unavailableTimeRepository,
            clock
        );
    }

    @Test
    @DisplayName("SCH-06 - Schedule 주차 범위를 벗어난 Shift가 있으면 공개할 수 없다")
    void publishScheduleThrowsExceptionWhenShiftIsOutsideScheduleWeek() {

        // given
        Long actorUserId = 1L;
        Long workplaceId = 10L;
        Long scheduleId = 20L;

        LocalDate weekStartDate =
            LocalDate.of(
                2026, 9, 21
            );

        LocalDateTime startAt =
            LocalDateTime.of(
                2026, 9, 20,
                10, 0
            );

        LocalDateTime endAt =
            LocalDateTime.of(
                2026, 9, 20,
                14, 0
            );

        Schedule schedule =
            mock(Schedule.class);

        Shift shift =
            mock(Shift.class);

        Workplace workplace =
            mock(Workplace.class);

        WorkplaceMember member =
            mock(WorkplaceMember.class);

        when(
            scheduleRepository.findById(
                scheduleId
            )
        ).thenReturn(
            Optional.of(schedule)
        );

        when(schedule.getWorkplace())
            .thenReturn(workplace);

        when(workplace.getId())
            .thenReturn(workplaceId);

        when(schedule.getStatus())
            .thenReturn(
                ScheduleStatus.DRAFT
            );

        when(schedule.getId())
            .thenReturn(scheduleId);

        when(schedule.getWeekStartDate())
            .thenReturn(weekStartDate);

        when(
            shiftRepository
                .findBySchedule_IdOrderByStartAtAscIdAsc(
                    scheduleId
                )
        ).thenReturn(
            List.of(shift)
        );

        when(shift.getMember())
            .thenReturn(member);

        when(member.getWorkplace())
            .thenReturn(workplace);

        when(member.getLeftAt())
            .thenReturn(null);

        when(shift.getStartAt())
            .thenReturn(startAt);

        when(shift.getEndAt())
            .thenReturn(endAt);

        // when
        BusinessException exception =
            assertThrows(
                BusinessException.class,
                () -> scheduleService.publishSchedule(
                    actorUserId,
                    workplaceId,
                    scheduleId,
                    false
                )
            );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(
                ScheduleErrorCode.INVALID_SHIFT_WEEK
            );

        verifyNoInteractions(
            unavailableTimeRepository,
            clock
        );
    }

    @Test
    @DisplayName("SCH-06 - 같은 Member의 Shift가 겹치면 공개할 수 없다")
    void publishScheduleThrowsExceptionWhenSameMemberShiftsOverlap() {

        // given
        Long actorUserId = 1L;
        Long workplaceId = 10L;
        Long scheduleId = 20L;
        Long memberId = 30L;

        LocalDate weekStartDate =
            LocalDate.of(
                2026, 9, 21
            );

        LocalDateTime firstStartAt =
            LocalDateTime.of(
                2026, 9, 21,
                9, 0
            );

        LocalDateTime firstEndAt =
            LocalDateTime.of(
                2026, 9, 21,
                14, 0
            );

        LocalDateTime secondStartAt =
            LocalDateTime.of(
                2026, 9, 21,
                13, 0
            );

        LocalDateTime secondEndAt =
            LocalDateTime.of(
                2026, 9, 21,
                18, 0
            );

        Schedule schedule =
            mock(Schedule.class);

        Shift firstShift =
            mock(Shift.class);

        Shift secondShift =
            mock(Shift.class);

        Workplace workplace =
            mock(Workplace.class);

        WorkplaceMember member =
            mock(WorkplaceMember.class);

        when(
            scheduleRepository.findById(
                scheduleId
            )
        ).thenReturn(
            Optional.of(schedule)
        );

        when(schedule.getWorkplace())
            .thenReturn(workplace);

        when(workplace.getId())
            .thenReturn(workplaceId);

        when(schedule.getStatus())
            .thenReturn(
                ScheduleStatus.DRAFT
            );

        when(schedule.getId())
            .thenReturn(scheduleId);

        when(schedule.getWeekStartDate())
            .thenReturn(weekStartDate);

        when(
            shiftRepository
                .findBySchedule_IdOrderByStartAtAscIdAsc(
                    scheduleId
                )
        ).thenReturn(
            List.of(
                firstShift,
                secondShift
            )
        );

        when(firstShift.getMember())
            .thenReturn(member);

        when(secondShift.getMember())
            .thenReturn(member);

        when(member.getId())
            .thenReturn(memberId);

        when(member.getWorkplace())
            .thenReturn(workplace);

        when(member.getLeftAt())
            .thenReturn(null);

        when(firstShift.getStartAt())
            .thenReturn(firstStartAt);

        when(firstShift.getEndAt())
            .thenReturn(firstEndAt);

        when(secondShift.getStartAt())
            .thenReturn(secondStartAt);

        when(secondShift.getEndAt())
            .thenReturn(secondEndAt);

        // when
        BusinessException exception =
            assertThrows(
                BusinessException.class,
                () -> scheduleService.publishSchedule(
                    actorUserId,
                    workplaceId,
                    scheduleId,
                    false
                )
            );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(
                ScheduleErrorCode.SHIFT_OVERLAP
            );

        verifyNoInteractions(
            unavailableTimeRepository,
            clock
        );
    }

    @Test
    @DisplayName("SCH-06 - 기존 공식 Shift와 충돌하면 공개할 수 없다")
    void publishScheduleThrowsExceptionWhenOfficialShiftConflicts() {

        // given
        Long actorUserId = 1L;
        Long workplaceId = 10L;
        Long scheduleId = 20L;
        Long userId = 30L;

        LocalDate weekStartDate =
            LocalDate.of(
                2026, 9, 21
            );

        LocalDateTime startAt =
            LocalDateTime.of(
                2026, 9, 21,
                10, 0
            );

        LocalDateTime endAt =
            LocalDateTime.of(
                2026, 9, 21,
                14, 0
            );

        Schedule schedule =
            mock(Schedule.class);

        Shift shift =
            mock(Shift.class);

        Workplace workplace =
            mock(Workplace.class);

        WorkplaceMember member =
            mock(WorkplaceMember.class);

        User user =
            mock(User.class);

        when(
            scheduleRepository.findById(
                scheduleId
            )
        ).thenReturn(
            Optional.of(schedule)
        );

        when(schedule.getWorkplace())
            .thenReturn(workplace);

        when(workplace.getId())
            .thenReturn(workplaceId);

        when(schedule.getStatus())
            .thenReturn(
                ScheduleStatus.DRAFT
            );

        when(schedule.getId())
            .thenReturn(scheduleId);

        when(schedule.getWeekStartDate())
            .thenReturn(weekStartDate);

        when(
            shiftRepository
                .findBySchedule_IdOrderByStartAtAscIdAsc(
                    scheduleId
                )
        ).thenReturn(
            List.of(shift)
        );

        when(shift.getMember())
            .thenReturn(member);

        when(member.getWorkplace())
            .thenReturn(workplace);

        when(member.getLeftAt())
            .thenReturn(null);

        when(member.getUser())
            .thenReturn(user);

        when(user.getId())
            .thenReturn(userId);

        when(shift.getStartAt())
            .thenReturn(startAt);

        when(shift.getEndAt())
            .thenReturn(endAt);

        when(
            shiftRepository
                .existsOverlappingOfficialShift(
                    userId,
                    ScheduleStatus.PUBLISHED,
                    startAt,
                    endAt,
                    null
                )
        ).thenReturn(true);

        // when
        BusinessException exception =
            assertThrows(
                BusinessException.class,
                () -> scheduleService.publishSchedule(
                    actorUserId,
                    workplaceId,
                    scheduleId,
                    false
                )
            );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(
                ScheduleErrorCode.OFFICIAL_SHIFT_CONFLICT
            );

        verify(shiftRepository)
            .existsOverlappingOfficialShift(
                userId,
                ScheduleStatus.PUBLISHED,
                startAt,
                endAt,
                null
            );

        verifyNoInteractions(
            unavailableTimeRepository,
            clock
        );
    }

    @Test
    @DisplayName("SCH-06 - UnavailableTime 충돌을 확인하지 않으면 공개할 수 없다")
    void publishScheduleThrowsExceptionWhenUnavailableConflictIsNotConfirmed() {

        // given
        Long actorUserId = 1L;
        Long workplaceId = 10L;
        Long scheduleId = 20L;
        Long userId = 30L;

        LocalDate weekStartDate =
            LocalDate.of(2026, 9, 21);

        LocalDateTime startAt =
            LocalDateTime.of(
                2026, 9, 21,
                10, 0
            );

        LocalDateTime endAt =
            LocalDateTime.of(
                2026, 9, 21,
                14, 0
            );

        Schedule schedule =
            mock(Schedule.class);

        Shift shift =
            mock(Shift.class);

        Workplace workplace =
            mock(Workplace.class);

        WorkplaceMember member =
            mock(WorkplaceMember.class);

        User user =
            mock(User.class);

        when(scheduleRepository.findById(scheduleId))
            .thenReturn(Optional.of(schedule));

        when(schedule.getId())
            .thenReturn(scheduleId);

        when(schedule.getWorkplace())
            .thenReturn(workplace);

        when(schedule.getWeekStartDate())
            .thenReturn(weekStartDate);

        when(schedule.getStatus())
            .thenReturn(ScheduleStatus.DRAFT);

        when(workplace.getId())
            .thenReturn(workplaceId);

        when(
            shiftRepository
                .findBySchedule_IdOrderByStartAtAscIdAsc(
                    scheduleId
                )
        ).thenReturn(
            List.of(shift)
        );

        when(shift.getMember())
            .thenReturn(member);

        when(shift.getStartAt())
            .thenReturn(startAt);

        when(shift.getEndAt())
            .thenReturn(endAt);

        when(member.getWorkplace())
            .thenReturn(workplace);

        when(member.getLeftAt())
            .thenReturn(null);

        when(member.getUser())
            .thenReturn(user);

        when(user.getId())
            .thenReturn(userId);

        when(
            unavailableTimeRepository
                .existsOverlappingUnavailableTime(
                    userId,
                    startAt,
                    endAt
                )
        ).thenReturn(true);

        // when
        BusinessException exception =
            assertThrows(
                BusinessException.class,
                () -> scheduleService.publishSchedule(
                    actorUserId,
                    workplaceId,
                    scheduleId,
                    false
                )
            );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(
                ShiftErrorCode.UNAVAILABLE_TIME_CONFLICT
            );

        verifyNoInteractions(clock);
    }

    @Test
    @DisplayName("SCH-06 - UnavailableTime 충돌을 확인하면 공개할 수 있다")
    void publishScheduleSucceedsWhenUnavailableConflictIsConfirmed() {

        // given
        Long actorUserId = 1L;
        Long workplaceId = 10L;
        Long scheduleId = 20L;
        Long userId = 30L;

        LocalDate weekStartDate =
            LocalDate.of(2026, 9, 21);

        LocalDateTime startAt =
            LocalDateTime.of(
                2026, 9, 21,
                10, 0
            );

        LocalDateTime endAt =
            LocalDateTime.of(
                2026, 9, 21,
                14, 0
            );

        LocalDateTime publishedAt =
            LocalDateTime.of(
                2026, 9, 19,
                18, 30
            );

        Schedule schedule =
            mock(Schedule.class);

        Shift shift =
            mock(Shift.class);

        Workplace workplace =
            mock(Workplace.class);

        WorkplaceMember member =
            mock(WorkplaceMember.class);

        User user =
            mock(User.class);

        when(scheduleRepository.findById(scheduleId))
            .thenReturn(Optional.of(schedule));

        when(schedule.getId())
            .thenReturn(scheduleId);

        when(schedule.getWorkplace())
            .thenReturn(workplace);

        when(schedule.getWeekStartDate())
            .thenReturn(weekStartDate);

        when(schedule.getStatus())
            .thenReturn(
                ScheduleStatus.DRAFT,
                ScheduleStatus.PUBLISHED
            );

        when(schedule.getPublishedAt())
            .thenReturn(publishedAt);

        when(workplace.getId())
            .thenReturn(workplaceId);

        when(
            shiftRepository
                .findBySchedule_IdOrderByStartAtAscIdAsc(
                    scheduleId
                )
        ).thenReturn(
            List.of(shift)
        );

        when(shift.getMember())
            .thenReturn(member);

        when(shift.getStartAt())
            .thenReturn(startAt);

        when(shift.getEndAt())
            .thenReturn(endAt);

        when(member.getWorkplace())
            .thenReturn(workplace);

        when(member.getLeftAt())
            .thenReturn(null);

        when(member.getUser())
            .thenReturn(user);

        when(user.getId())
            .thenReturn(userId);

        when(
            unavailableTimeRepository
                .existsOverlappingUnavailableTime(
                    userId,
                    startAt,
                    endAt
                )
        ).thenReturn(true);

        when(clock.instant())
            .thenReturn(
                Instant.parse(
                    "2026-09-19T09:30:00Z"
                )
            );

        when(clock.getZone())
            .thenReturn(
                ZoneId.of("Asia/Seoul")
            );

        // when
        SchedulePublishResponse result =
            scheduleService.publishSchedule(
                actorUserId,
                workplaceId,
                scheduleId,
                true
            );

        // then
        assertThat(result.status())
            .isEqualTo(
                ScheduleStatus.PUBLISHED
            );

        assertThat(result.publishedAt())
            .isEqualTo(publishedAt);

        verify(schedule)
            .publish(publishedAt);
    }

}