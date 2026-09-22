package com.merge.backend.domain.substitute.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.merge.backend.domain.shift.entity.Schedule;
import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.entity.ShiftStatus;
import com.merge.backend.domain.shift.repository.ShiftRepository;
import com.merge.backend.domain.substitute.dto.SubstituteRequestCreateResponse;
import com.merge.backend.domain.substitute.entity.RequestStatus;
import com.merge.backend.domain.substitute.entity.SubstituteRequest;
import com.merge.backend.domain.substitute.exception.SubstituteErrorCode;
import com.merge.backend.domain.substitute.repository.SubstituteRequestRepository;
import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.workplace.entity.Workplace;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;
import com.merge.backend.global.exception.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SubstituteRequestServiceTest {

    private static final Long SHIFT_ID = 1L;
    private static final Long REQUESTER_USER_ID = 10L;
    private static final Long OTHER_USER_ID = 20L;
    private static final Long SAVED_REQUEST_ID = 50L;
    private static final List<RequestStatus> ACTIVE_STATUSES =
        List.of(RequestStatus.OPEN, RequestStatus.ACCEPTED);

    @Mock
    private ShiftRepository shiftRepository;

    @Mock
    private SubstituteRequestRepository substituteRequestRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private SubstituteRequestService substituteRequestService;

    private void stubClockAsNow() {
        given(clock.instant()).willReturn(Instant.now());
        given(clock.getZone()).willReturn(ZoneId.of("Asia/Seoul"));
    }

    @Test
    @DisplayName("SUB-01 - 검증을 통과하면 대타 요청이 OPEN으로 저장된다")
    void test1() {
        Shift shift = createShift(
            publishedSchedule(), REQUESTER_USER_ID, futureStartAt(), ShiftStatus.SCHEDULED
        );
        given(shiftRepository.findById(SHIFT_ID)).willReturn(Optional.of(shift));
        stubClockAsNow();
        given(substituteRequestRepository.existsByShift_IdAndStatusIn(SHIFT_ID, ACTIVE_STATUSES))
            .willReturn(false);
        given(substituteRequestRepository.save(any(SubstituteRequest.class)))
            .willAnswer(invocation -> {
                SubstituteRequest request = invocation.getArgument(0);
                ReflectionTestUtils.setField(request, "id", SAVED_REQUEST_ID);
                return request;
            });

        SubstituteRequestCreateResponse response =
            substituteRequestService.create(SHIFT_ID, REQUESTER_USER_ID);

        ArgumentCaptor<SubstituteRequest> captor =
            ArgumentCaptor.forClass(SubstituteRequest.class);
        verify(substituteRequestRepository).save(captor.capture());
        SubstituteRequest saved = captor.getValue();
        assertThat(saved.getShift()).isSameAs(shift);
        assertThat(saved.getRequesterMember()).isSameAs(shift.getMember());
        assertThat(saved.getStatus()).isEqualTo(RequestStatus.OPEN);

        assertThat(response.requestCreated()).isTrue();
        assertThat(response.requestId()).isEqualTo(SAVED_REQUEST_ID);
        assertThat(response.shiftId()).isEqualTo(SHIFT_ID);
        assertThat(response.status()).isEqualTo(RequestStatus.OPEN);
        assertThat(response.candidateCount()).isZero();
    }

    @Test
    @DisplayName("SUB-01 실패 - 존재하지 않는 Shift")
    void test2() {
        given(shiftRepository.findById(SHIFT_ID)).willReturn(Optional.empty());

        assertCreateFails(SubstituteErrorCode.SHIFT_NOT_FOUND);
    }

    @Test
    @DisplayName("SUB-01 실패 - 공개되지 않은 근무표의 Shift")
    void test3() {
        Shift shift = createShift(
            draftSchedule(), REQUESTER_USER_ID, futureStartAt(), ShiftStatus.SCHEDULED
        );
        given(shiftRepository.findById(SHIFT_ID)).willReturn(Optional.of(shift));

        assertCreateFails(SubstituteErrorCode.SHIFT_NOT_PUBLISHED);
    }

    @Test
    @DisplayName("SUB-01 실패 - 취소된 Shift")
    void test4() {
        Shift shift = createShift(
            publishedSchedule(), REQUESTER_USER_ID, futureStartAt(), ShiftStatus.CANCELLED
        );
        given(shiftRepository.findById(SHIFT_ID)).willReturn(Optional.of(shift));

        assertCreateFails(SubstituteErrorCode.SHIFT_CANCELLED);
    }

    @Test
    @DisplayName("SUB-01 실패 - 본인의 Shift가 아님")
    void test5() {
        Shift shift = createShift(
            publishedSchedule(), OTHER_USER_ID, futureStartAt(), ShiftStatus.SCHEDULED
        );
        given(shiftRepository.findById(SHIFT_ID)).willReturn(Optional.of(shift));

        assertCreateFails(SubstituteErrorCode.NOT_OWN_SHIFT);
    }

    @Test
    @DisplayName("SUB-01 실패 - 이미 시작된 Shift")
    void test6() {
        Shift shift = createShift(
            publishedSchedule(),
            REQUESTER_USER_ID,
            LocalDateTime.now().minusMinutes(1),
            ShiftStatus.SCHEDULED
        );
        given(shiftRepository.findById(SHIFT_ID)).willReturn(Optional.of(shift));
        stubClockAsNow();

        assertCreateFails(SubstituteErrorCode.SHIFT_ALREADY_STARTED);
    }

    @Test
    @DisplayName("SUB-01 실패 - 이미 진행 중인(OPEN/ACCEPTED) 요청이 있음")
    void test7() {
        Shift shift = createShift(
            publishedSchedule(), REQUESTER_USER_ID, futureStartAt(), ShiftStatus.SCHEDULED
        );
        given(shiftRepository.findById(SHIFT_ID)).willReturn(Optional.of(shift));
        stubClockAsNow();
        given(substituteRequestRepository.existsByShift_IdAndStatusIn(SHIFT_ID, ACTIVE_STATUSES))
            .willReturn(true);

        assertCreateFails(SubstituteErrorCode.ACTIVE_REQUEST_EXISTS);
    }

    private void assertCreateFails(SubstituteErrorCode expected) {
        assertThatThrownBy(() -> substituteRequestService.create(SHIFT_ID, REQUESTER_USER_ID))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(expected);

        verify(substituteRequestRepository, never()).save(any(SubstituteRequest.class));
    }

    private Shift createShift(
        Schedule schedule,
        Long ownerUserId,
        LocalDateTime startAt,
        ShiftStatus status
    ) {
        WorkplaceMember owner = new WorkplaceMember(
            schedule.getWorkplace(),
            new User(ownerUserId, "owner@example.com", "담당자"),
            WorkplaceRole.EMPLOYEE,
            LocalDateTime.now().minusDays(30),
            null
        );
        return new Shift(schedule, owner, startAt, startAt.plusHours(4), status);
    }

    private Schedule draftSchedule() {
        return new Schedule(new Workplace("SWITCH 카페", "INVITE01"), LocalDate.now());
    }

    private Schedule publishedSchedule() {
        Schedule schedule = draftSchedule();
        schedule.publish(LocalDateTime.now());
        return schedule;
    }

    private LocalDateTime futureStartAt() {
        return LocalDateTime.now().plusDays(1);
    }
}
