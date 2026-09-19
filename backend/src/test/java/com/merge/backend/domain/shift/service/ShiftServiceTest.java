package com.merge.backend.domain.shift.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import com.merge.backend.domain.shift.entity.Schedule;
import com.merge.backend.domain.shift.entity.ScheduleStatus;
import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.entity.ShiftStatus;
import com.merge.backend.domain.shift.exception.ShiftErrorCode;
import com.merge.backend.domain.shift.repository.ScheduleRepository;
import com.merge.backend.domain.shift.repository.ShiftRepository;
import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.workplace.entity.Workplace;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.exception.WorkplaceErrorCode;
import com.merge.backend.domain.workplace.repository.WorkplaceRepository;
import com.merge.backend.domain.workplace.service.WorkplaceMemberService;
import com.merge.backend.global.exception.BusinessException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShiftServiceTest {

    @InjectMocks
    private ShiftService shiftService;

    @Mock
    private ShiftRepository shiftRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private WorkplaceRepository workplaceRepository;

    @Mock
    private WorkplaceMemberService workplaceMemberService;

    @Test
    @DisplayName("SCH-01 - Pattern에서 전달된 정보로 SCHEDULED Shift를 저장한다")
    void createFromValidatedPattern_SavesScheduledShift() {
        // given
        Schedule schedule = mock(Schedule.class);
        WorkplaceMember member = mock(WorkplaceMember.class);

        LocalDateTime startAt = LocalDateTime.of(
            2026, 9, 23, 9, 0
        );
        LocalDateTime endAt = LocalDateTime.of(
            2026, 9, 23, 14, 0
        );

        Shift savedShift = mock(Shift.class);

        when(shiftRepository.save(any(Shift.class)))
            .thenReturn(savedShift);

        // when
        Shift result = shiftService.createFromValidatedPattern(
            schedule,
            member,
            startAt,
            endAt
        );

        // then
        ArgumentCaptor<Shift> shiftCaptor =
            ArgumentCaptor.forClass(Shift.class);

        verify(shiftRepository).save(shiftCaptor.capture());

        Shift shiftToSave = shiftCaptor.getValue();

        assertThat(shiftToSave.getSchedule()).isSameAs(schedule);
        assertThat(shiftToSave.getMember()).isSameAs(member);
        assertThat(shiftToSave.getStartAt()).isEqualTo(startAt);
        assertThat(shiftToSave.getEndAt()).isEqualTo(endAt);
        assertThat(shiftToSave.getStatus())
            .isEqualTo(ShiftStatus.SCHEDULED);

        assertThat(result).isSameAs(savedShift);
    }

    @Test
    @DisplayName("근무 상세 조회 성공 - 본인의 확정된 근무 정보 반환")
    void detail_Success() {
        // given
        Long shiftId = 1L;
        Long currentUserId = 100L;

        User user = mock(User.class);
        given(user.getId()).willReturn(currentUserId);

        WorkplaceMember member = mock(WorkplaceMember.class);
        given(member.getUser()).willReturn(user);

        Schedule schedule = mock(Schedule.class);
        given(schedule.getStatus()).willReturn(ScheduleStatus.PUBLISHED);

        Shift shift = mock(Shift.class);
        given(shift.getSchedule()).willReturn(schedule);
        given(shift.getStatus()).willReturn(ShiftStatus.SCHEDULED);
        given(shift.getMember()).willReturn(member);

        given(shiftRepository.findById(shiftId)).willReturn(Optional.of(shift));

        // when
        Shift result = shiftService.detail(shiftId, currentUserId);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(shift);
    }

    @Test
    @DisplayName("근무 상세 조회 실패 - 존재하지 않는 근무 ID인 경우")
    void detail_Fail_NotFoundShift() {
        // given
        Long shiftId = 999L;
        Long currentUserId = 100L;

        given(shiftRepository.findById(shiftId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> shiftService.detail(shiftId, currentUserId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(ShiftErrorCode.NOT_FOUND_SHIFT_ERROR.getMessage());
    }

    @Test
    @DisplayName("근무 상세 조회 실패 - 스케줄이 PUBLISHED 상태가 아닌 경우")
    void detail_Fail_NotPublishedSchedule() {
        // given
        Long shiftId = 1L;
        Long currentUserId = 100L;

        Schedule schedule = mock(Schedule.class);
        given(schedule.getStatus()).willReturn(ScheduleStatus.DRAFT);

        Shift shift = mock(Shift.class);
        given(shift.getSchedule()).willReturn(schedule);

        given(shiftRepository.findById(shiftId)).willReturn(Optional.of(shift));

        // when & then
        assertThatThrownBy(() -> shiftService.detail(shiftId, currentUserId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(ShiftErrorCode.NOT_PUBLISHED_SHIFT.getMessage());
    }

    @Test
    @DisplayName("근무 상세 조회 실패 - 취소된 근무인 경우")
    void detail_Fail_CanceledShift() {
        // given
        Long shiftId = 1L;
        Long currentUserId = 100L;

        Schedule schedule = mock(Schedule.class);
        given(schedule.getStatus()).willReturn(ScheduleStatus.PUBLISHED);

        Shift shift = mock(Shift.class);
        given(shift.getSchedule()).willReturn(schedule);
        given(shift.getStatus()).willReturn(ShiftStatus.CANCELLED);

        given(shiftRepository.findById(shiftId)).willReturn(Optional.of(shift));

        // when & then
        assertThatThrownBy(() -> shiftService.detail(shiftId, currentUserId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(ShiftErrorCode.IS_CANCELED_SHIFT.getMessage());
    }

    @Test
    @DisplayName("근무 상세 조회 실패 - 다른 사용자의 근무를 조회하려는 경우")
    void detail_Fail_ForbiddenAccess() {
        // given
        Long shiftId = 1L;
        Long currentUserId = 100L;
        Long otherUserId = 200L;

        User user = mock(User.class);
        given(user.getId()).willReturn(otherUserId);

        WorkplaceMember member = mock(WorkplaceMember.class);
        given(member.getUser()).willReturn(user);

        Schedule schedule = mock(Schedule.class);
        given(schedule.getStatus()).willReturn(ScheduleStatus.PUBLISHED);

        Shift shift = mock(Shift.class);
        given(shift.getSchedule()).willReturn(schedule);
        given(shift.getStatus()).willReturn(ShiftStatus.SCHEDULED);
        given(shift.getMember()).willReturn(member);

        given(shiftRepository.findById(shiftId)).willReturn(Optional.of(shift));

        // when & then
        assertThatThrownBy(() -> shiftService.detail(shiftId, currentUserId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(ShiftErrorCode.FORBIDDEN_ACCESS.getMessage());
    }

    @Test
    @DisplayName("성공 - DRAFT 상태 스케줄의 근무를 정상적으로 삭제한다.")
    void delete_success() {
        // given
        Long workplaceId = 1L;
        Long scheduleId = 10L;
        Long shiftId = 100L;
        Long actorId = 200L;

        Workplace workplace = mock(Workplace.class);
        Schedule schedule = mock(Schedule.class);
        Shift shift = mock(Shift.class);
        WorkplaceMember managerMember = mock(WorkplaceMember.class);

        // Workplace 존재
        when(workplaceRepository.findById(workplaceId)).thenReturn(Optional.of(workplace));

        // 매니저 권한 검증 Mocking
        when(workplaceMemberService.requireManager(actorId, workplaceId)).thenReturn(managerMember);

        // Schedule 검증
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(schedule.getWorkplace()).thenReturn(workplace);
        when(workplace.getId()).thenReturn(workplaceId);
        when(schedule.getStatus()).thenReturn(ScheduleStatus.DRAFT);

        // Shift 검증
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(shift));
        when(shift.getSchedule()).thenReturn(schedule);
        when(schedule.getId()).thenReturn(scheduleId);

        // when
        shiftService.delete(workplaceId, scheduleId, shiftId, actorId);

        // then
        verify(shiftRepository, times(1)).deleteById(shiftId);
    }

    @Test
    @DisplayName("실패 - 매니저 권한이 없으면 FORBIDDEN_ACCESS 예외가 발생한다.")
    void delete_forbidden_whenNotManager() {
        // given
        Long workplaceId = 1L;
        Long scheduleId = 10L;
        Long shiftId = 100L;
        Long actorId = 200L;

        // 매니저 권한 부족 시 예외 발생 스텁 설정
        doThrow(new BusinessException(WorkplaceErrorCode.MANAGER_REQUIRED))
            .when(workplaceMemberService).requireManager(actorId, workplaceId);

        // when & then
        assertThrows(
            BusinessException.class,
            () -> shiftService.delete(workplaceId, scheduleId, shiftId, actorId)
        );

        verify(shiftRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("실패 - 스케줄 상태가 DRAFT가 아니면 INVALID_STATUS_VALUE 예외가 발생한다.")
    void delete_fail_whenScheduleNotDraft() {
        // given
        Long workplaceId = 1L;
        Long scheduleId = 10L;
        Long shiftId = 100L;
        Long actorId = 200L;

        Workplace workplace = mock(Workplace.class);
        Schedule schedule = mock(Schedule.class);
        WorkplaceMember managerMember = mock(WorkplaceMember.class);

        when(workplaceRepository.findById(workplaceId)).thenReturn(Optional.of(workplace));
        when(workplaceMemberService.requireManager(actorId, workplaceId)).thenReturn(managerMember);

        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(schedule.getWorkplace()).thenReturn(workplace);
        when(workplace.getId()).thenReturn(workplaceId);
        when(schedule.getStatus()).thenReturn(ScheduleStatus.PUBLISHED);

        // when & then
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> shiftService.delete(workplaceId, scheduleId, shiftId, actorId)
        );

        assertThat(exception.getErrorCode()).isEqualTo(ShiftErrorCode.INVALID_STATUS_VALUE);
        verify(shiftRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("실패 - 삭제하려는 근무가 해당 스케줄 소속이 아니면 INVALID_SHIFT_VALUE 예외가 발생한다.")
    void delete_fail_whenShiftNotInSchedule() {
        // given
        Long workplaceId = 1L;
        Long scheduleId = 10L;
        Long otherScheduleId = 99L;
        Long shiftId = 100L;
        Long actorId = 200L;

        Workplace workplace = mock(Workplace.class);
        Schedule schedule = mock(Schedule.class);
        Schedule otherSchedule = mock(Schedule.class);
        Shift shift = mock(Shift.class);
        WorkplaceMember managerMember = mock(WorkplaceMember.class);

        when(workplaceRepository.findById(workplaceId)).thenReturn(Optional.of(workplace));
        when(workplaceMemberService.requireManager(actorId, workplaceId)).thenReturn(managerMember);

        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(schedule.getWorkplace()).thenReturn(workplace);
        when(workplace.getId()).thenReturn(workplaceId);
        when(schedule.getStatus()).thenReturn(ScheduleStatus.DRAFT);

        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(shift));
        when(shift.getSchedule()).thenReturn(otherSchedule);
        when(otherSchedule.getId()).thenReturn(otherScheduleId);

        // when & then
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> shiftService.delete(workplaceId, scheduleId, shiftId, actorId)
        );

        assertThat(exception.getErrorCode()).isEqualTo(ShiftErrorCode.INVALID_SHIFT_VALUE);
        verify(shiftRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("주 시작 일자와 사용자 ID로 주간 근무 목록을 성공적으로 조회한다")
    void list_Success() {
        // given
        LocalDate weekStartDate = LocalDate.of(2026, 9, 21);
        Long currentUserId = 1L;

        Shift mockShift1 = mock(Shift.class);
        Shift mockShift2 = mock(Shift.class);
        List<Shift> expectedShifts = List.of(mockShift1, mockShift2);

        given(shiftRepository.findAllByWeekStartDateAndCurrentUserId(weekStartDate, currentUserId))
            .willReturn(expectedShifts);

        // when
        List<Shift> result = shiftService.list(weekStartDate, currentUserId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(expectedShifts);
        verify(shiftRepository).findAllByWeekStartDateAndCurrentUserId(
            weekStartDate, currentUserId);
    }

    @Test
    @DisplayName("월요일이 아닌 날짜로 주간 근무를 조회하면 실패한다")
    void list_Fail_WhenWeekStartDateIsNotMonday() {
        // given
        LocalDate weekStartDate = LocalDate.of(2026, 9, 22);
        Long currentUserId = 1L;

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> shiftService.list(weekStartDate, currentUserId)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ShiftErrorCode.INVALID_INPUT_VALUE);

        verifyNoInteractions(shiftRepository);
    }

}