package com.merge.backend.domain.shift.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.merge.backend.domain.shift.entity.Schedule;
import com.merge.backend.domain.shift.entity.ScheduleStatus;
import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.entity.ShiftStatus;
import com.merge.backend.domain.shift.exception.ShiftErrorCode;
import com.merge.backend.domain.shift.repository.ShiftRepository;
import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.global.exception.BusinessException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShiftServiceTest {

    @InjectMocks
    private ShiftService shiftService;

    @Mock
    private ShiftRepository shiftRepository;

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
        Long otherUserId = 200L; // 다른 유저 ID

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
}
