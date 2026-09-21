package com.merge.backend.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.merge.backend.domain.shift.entity.Schedule;
import com.merge.backend.domain.shift.entity.ScheduleStatus;
import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.entity.ShiftStatus;
import com.merge.backend.domain.shift.exception.ShiftErrorCode;
import com.merge.backend.domain.shift.repository.ShiftRepository;
import com.merge.backend.domain.shift.repository.UnavailableTimeRepository;
import com.merge.backend.domain.substitute.entity.CandidateStatus;
import com.merge.backend.domain.substitute.entity.RequestStatus;
import com.merge.backend.domain.substitute.entity.SubstituteCandidate;
import com.merge.backend.domain.substitute.entity.SubstituteRequest;
import com.merge.backend.domain.substitute.exception.SubstituteRequestErrorCode;
import com.merge.backend.domain.substitute.repository.SubstituteCandidateRepository;
import com.merge.backend.domain.substitute.repository.SubstituteRequestRepository;
import com.merge.backend.domain.substitute.service.SubstituteRequestService;
import com.merge.backend.domain.workplace.entity.Workplace;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.repository.WorkplaceRepository;
import com.merge.backend.domain.workplace.service.WorkplaceMemberService;
import com.merge.backend.global.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubstituteRequestServiceTest {

    @InjectMocks
    private SubstituteRequestService substituteRequestService;

    @Mock
    private SubstituteRequestRepository substituteRequestRepository;

    @Mock
    private SubstituteCandidateRepository substituteCandidateRepository;

    @Mock
    private ShiftRepository shiftRepository;

    @Mock
    private WorkplaceRepository workplaceRepository;

    @Mock
    private UnavailableTimeRepository unavailableTimeRepository;

    @Mock
    private WorkplaceMemberService workplaceMemberService;

    private Long requestId;
    private Long actorId;
    private Long workplaceId;
    private Long candidateMemberId;

    private Workplace workplace;
    private WorkplaceMember requesterMember;
    private WorkplaceMember candidateMember;
    private Schedule schedule;
    private Shift shift;
    private SubstituteRequest request;
    private SubstituteCandidate candidate;

    @BeforeEach
    void setUp() {
        requestId = 1L;
        actorId = 10L;
        workplaceId = 100L;
        candidateMemberId = 20L;

        // Mock 객체 생성
        workplace = mock(Workplace.class);
        requesterMember = mock(WorkplaceMember.class);
        candidateMember = mock(WorkplaceMember.class);
        schedule = mock(Schedule.class);
        shift = mock(Shift.class);
        request = mock(SubstituteRequest.class);
        candidate = mock(SubstituteCandidate.class);
    }

    @Test
    @DisplayName("대체 근무 승인 성공")
    void approve_success() {
        // given

        given(workplace.getId()).willReturn(workplaceId);
        given(requesterMember.getWorkplace()).willReturn(workplace);
        given(request.getRequesterMember()).willReturn(requesterMember);
        given(request.getShift()).willReturn(shift);

        given(request.getStatus()).willReturn(RequestStatus.ACCEPTED);
        given(schedule.getStatus()).willReturn(ScheduleStatus.PUBLISHED);
        given(shift.getSchedule()).willReturn(schedule);
        given(shift.getStatus()).willReturn(ShiftStatus.SCHEDULED);
        given(shift.getStartAt()).willReturn(LocalDateTime.now().plusDays(1));
        given(shift.getEndAt()).willReturn(LocalDateTime.now().plusDays(1).plusHours(8));
        given(shift.getMember()).willReturn(requesterMember);

        given(candidateMember.getId()).willReturn(candidateMemberId);
        given(candidateMember.getWorkplace()).willReturn(workplace);
        given(candidateMember.getLeftAt()).willReturn(null);
        given(candidate.getMember()).willReturn(candidateMember);

        given(substituteRequestRepository.findById(requestId)).willReturn(Optional.of(request));
        WorkplaceMember managerMember = mock(WorkplaceMember.class);
        given(workplaceMemberService.requireManager(
            actorId, workplaceId)).willReturn(managerMember);
        given(workplaceRepository.findById(workplaceId)).willReturn(Optional.of(workplace));
        given(substituteCandidateRepository.findByRequestIdAndStatus(
            requestId, CandidateStatus.ACCEPTED))
            .willReturn(candidate);

        given(shiftRepository.existsConflictingShift(
            eq(candidateMemberId), any(), any())).willReturn(false);
        given(unavailableTimeRepository.existsOverlappingUnavailableTime(
            eq(actorId), any(), any())).willReturn(false);
        given(substituteCandidateRepository.existsConflictingActiveSubstitute(
            eq(candidateMemberId), eq(requestId), any(), any()))
            .willReturn(false);

        // when
        SubstituteRequest result = substituteRequestService.approve(requestId, actorId);

        // then
        assertThat(result).isNotNull();
        verify(request).approveRequest(eq(candidateMember), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("요청 상태가 ACCEPTED가 아니면 예외가 발생한다")
    void approve_fail_invalid_request_status() {
        // given
        given(request.getStatus()).willReturn(RequestStatus.OPEN);
        given(substituteRequestRepository.findById(requestId)).willReturn(Optional.of(request));

        // when & then
        assertThatThrownBy(() -> substituteRequestService.approve(requestId, actorId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(SubstituteRequestErrorCode.INVALID_REQUEST.getMessage());
    }

    @Test
    @DisplayName("근무 시작일이 이미 지난 경우 예외가 발생한다")
    void approve_fail_past_shift_start_time() {

        given(workplace.getId()).willReturn(workplaceId);
        given(requesterMember.getWorkplace()).willReturn(workplace);
        given(request.getRequesterMember()).willReturn(requesterMember);
        given(request.getShift()).willReturn(shift);
        // given
        given(request.getStatus()).willReturn(RequestStatus.ACCEPTED);
        given(schedule.getStatus()).willReturn(ScheduleStatus.PUBLISHED);
        given(shift.getSchedule()).willReturn(schedule);
        given(shift.getStatus()).willReturn(ShiftStatus.SCHEDULED);
        given(shift.getStartAt()).willReturn(LocalDateTime.now().minusHours(1));

        given(substituteRequestRepository.findById(requestId)).willReturn(Optional.of(request));

        // when & then
        assertThatThrownBy(() -> substituteRequestService.approve(requestId, actorId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(ShiftErrorCode.INVALID_TIME_VALUE.getMessage());
    }

    @Test
    @DisplayName("후보자가 해당 근무지 소속이 아니거나 퇴사한 경우 예외가 발생한다")
    void approve_fail_candidate_not_in_workplace() {

        given(workplace.getId()).willReturn(workplaceId);
        given(requesterMember.getWorkplace()).willReturn(workplace);
        given(request.getRequesterMember()).willReturn(requesterMember);
        given(request.getShift()).willReturn(shift);
        // given
        given(request.getStatus()).willReturn(RequestStatus.ACCEPTED);
        given(schedule.getStatus()).willReturn(ScheduleStatus.PUBLISHED);
        given(shift.getSchedule()).willReturn(schedule);
        given(shift.getStatus()).willReturn(ShiftStatus.SCHEDULED);
        given(shift.getStartAt()).willReturn(LocalDateTime.now().plusDays(1));
        given(shift.getMember()).willReturn(requesterMember);

        Workplace otherWorkplace = mock(Workplace.class);
        given(candidateMember.getWorkplace()).willReturn(otherWorkplace);
        given(candidate.getMember()).willReturn(candidateMember);

        given(substituteRequestRepository.findById(requestId)).willReturn(Optional.of(request));
        given(workplaceRepository.findById(workplaceId)).willReturn(Optional.of(workplace));
        given(substituteCandidateRepository.findByRequestIdAndStatus(
            requestId, CandidateStatus.ACCEPTED))
            .willReturn(candidate);

        // when & then
        assertThatThrownBy(() -> substituteRequestService.approve(requestId, actorId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(ShiftErrorCode.INVALID_WORKPLACE_MEMBER_VALUE.getMessage());
    }

    @Test
    @DisplayName("후보자에게 충돌하는 기존 Shift가 존재하는 경우 예외가 발생한다")
    void approve_fail_conflicting_shift() {
        // given
        given(workplace.getId()).willReturn(workplaceId);
        given(requesterMember.getWorkplace()).willReturn(workplace);
        given(request.getRequesterMember()).willReturn(requesterMember);
        given(request.getShift()).willReturn(shift);
        given(request.getStatus()).willReturn(RequestStatus.ACCEPTED);
        given(schedule.getStatus()).willReturn(ScheduleStatus.PUBLISHED);
        given(shift.getSchedule()).willReturn(schedule);
        given(shift.getStatus()).willReturn(ShiftStatus.SCHEDULED);
        given(shift.getStartAt()).willReturn(LocalDateTime.now().plusDays(1));
        given(shift.getEndAt()).willReturn(LocalDateTime.now().plusDays(1).plusHours(8));
        given(shift.getMember()).willReturn(requesterMember);

        given(candidateMember.getId()).willReturn(candidateMemberId);
        given(candidateMember.getWorkplace()).willReturn(workplace);
        given(candidateMember.getLeftAt()).willReturn(null);
        given(candidate.getMember()).willReturn(candidateMember);

        given(substituteRequestRepository.findById(requestId)).willReturn(Optional.of(request));
        given(workplaceRepository.findById(workplaceId)).willReturn(Optional.of(workplace));
        given(substituteCandidateRepository.findByRequestIdAndStatus(
            requestId, CandidateStatus.ACCEPTED))
            .willReturn(candidate);

        given(shiftRepository.existsConflictingShift(
            eq(candidateMemberId), any(), any())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> substituteRequestService.approve(requestId, actorId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(SubstituteRequestErrorCode.CONFLICT_SHIFT.getMessage());
    }

    @Test
    @DisplayName("후보자가 다른 활성 대타 약속과 시간 충돌이 있는 경우 예외가 발생한다")
    void approve_fail_conflicting_active_substitute() {
        // given
        given(workplace.getId()).willReturn(workplaceId);
        given(requesterMember.getWorkplace()).willReturn(workplace);
        given(request.getRequesterMember()).willReturn(requesterMember);
        given(request.getShift()).willReturn(shift);
        given(request.getStatus()).willReturn(RequestStatus.ACCEPTED);
        given(schedule.getStatus()).willReturn(ScheduleStatus.PUBLISHED);
        given(shift.getSchedule()).willReturn(schedule);
        given(shift.getStatus()).willReturn(ShiftStatus.SCHEDULED);
        given(shift.getStartAt()).willReturn(LocalDateTime.now().plusDays(1));
        given(shift.getEndAt()).willReturn(LocalDateTime.now().plusDays(1).plusHours(8));
        given(shift.getMember()).willReturn(requesterMember);

        given(candidateMember.getId()).willReturn(candidateMemberId);
        given(candidateMember.getWorkplace()).willReturn(workplace);
        given(candidateMember.getLeftAt()).willReturn(null);
        given(candidate.getMember()).willReturn(candidateMember);

        given(substituteRequestRepository.findById(requestId)).willReturn(Optional.of(request));
        given(workplaceRepository.findById(workplaceId)).willReturn(Optional.of(workplace));
        given(substituteCandidateRepository.findByRequestIdAndStatus(
            requestId, CandidateStatus.ACCEPTED))
            .willReturn(candidate);

        given(shiftRepository.existsConflictingShift(
            eq(candidateMemberId), any(), any())).willReturn(false);
        given(unavailableTimeRepository.existsOverlappingUnavailableTime(
            eq(actorId), any(), any())).willReturn(false);
        given(substituteCandidateRepository.existsConflictingActiveSubstitute(
            eq(candidateMemberId), eq(requestId), any(), any()))
            .willReturn(true);

        // when & then
        assertThatThrownBy(() -> substituteRequestService.approve(requestId, actorId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(
                SubstituteRequestErrorCode.CONFLICT_ACTIVE_SUBSTITUTE.getMessage());
    }
}
