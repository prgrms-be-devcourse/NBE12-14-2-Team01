package com.merge.backend.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.merge.backend.domain.shift.entity.Schedule;
import com.merge.backend.domain.shift.entity.ScheduleStatus;
import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.entity.ShiftStatus;
import com.merge.backend.domain.shift.exception.ShiftErrorCode;
import com.merge.backend.domain.shift.repository.ShiftRepository;
import com.merge.backend.domain.shift.repository.UnavailableTimeRepository;
import com.merge.backend.domain.substitute.dto.SubstituteRequestListResponse;
import com.merge.backend.domain.substitute.entity.CandidateStatus;
import com.merge.backend.domain.substitute.entity.RequestStatus;
import com.merge.backend.domain.substitute.entity.SubstituteCandidate;
import com.merge.backend.domain.substitute.entity.SubstituteRequest;
import com.merge.backend.domain.substitute.exception.SubstituteRequestErrorCode;
import com.merge.backend.domain.substitute.repository.SubstituteCandidateRepository;
import com.merge.backend.domain.substitute.repository.SubstituteRequestRepository;
import com.merge.backend.domain.substitute.service.SubstituteRequestService;
import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.workplace.entity.Workplace;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.exception.WorkplaceErrorCode;
import com.merge.backend.domain.workplace.repository.WorkplaceRepository;
import com.merge.backend.domain.workplace.service.WorkplaceMemberService;
import com.merge.backend.global.exception.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
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

    @Spy
    private Clock clock = Clock.fixed(
        Instant.parse("2026-09-22T12:00:00Z"),
        ZoneId.of("Asia/Seoul")
    );

    private Long requestId;
    private Long actorId;
    private Long workplaceId;
    private Long requesterMemberId;

    private Long candidateUserId;
    private WorkplaceMember member;
    private Workplace workplace;
    private WorkplaceMember requesterMember;
    private WorkplaceMember candidateMember;
    private User candidateUser;
    private Schedule schedule;
    private Shift shift;
    private SubstituteRequest request;
    private SubstituteCandidate candidate;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        requestId = 1L;
        actorId = 10L;
        workplaceId = 100L;
        requesterMemberId = 15L;
        candidateUserId = 200L;
        now = LocalDateTime.now(clock);

        workplace = mock(Workplace.class);
        member = mock(WorkplaceMember.class);
        requesterMember = mock(WorkplaceMember.class);
        candidateMember = mock(WorkplaceMember.class);
        candidateUser = mock(User.class);
        schedule = mock(Schedule.class);
        shift = mock(Shift.class);
        request = mock(SubstituteRequest.class);
        candidate = mock(SubstituteCandidate.class);
    }

    private void setupValidRequestBasicInfo() {
        given(workplace.getId()).willReturn(workplaceId);
        given(schedule.getWorkplace()).willReturn(workplace);
        given(shift.getSchedule()).willReturn(schedule);
        given(request.getShift()).willReturn(shift);
        given(request.getStatus()).willReturn(RequestStatus.ACCEPTED);
    }

    private void setupValidRequestFullInfo() {
        setupValidRequestBasicInfo();
        given(schedule.getStatus()).willReturn(ScheduleStatus.PUBLISHED);
        given(shift.getStatus()).willReturn(ShiftStatus.SCHEDULED);
        given(requesterMember.getId()).willReturn(requesterMemberId);
        given(shift.getMember()).willReturn(requesterMember);
        given(request.getRequesterMember()).willReturn(requesterMember);
    }

    private void setupValidCandidateInfo() {
        given(candidateMember.getWorkplace()).willReturn(workplace);
        given(candidateMember.getLeftAt()).willReturn(null);
        given(candidateUser.getId()).willReturn(candidateUserId);
        given(candidateMember.getUser()).willReturn(candidateUser);
        given(candidate.getMember()).willReturn(candidateMember);
    }

    @Test
    @DisplayName("대체 근무 승인 성공")
    void approve_success() {
        // given
        setupValidRequestFullInfo();
        setupValidCandidateInfo();

        given(shift.getStartAt()).willReturn(now.plusDays(1));
        given(shift.getEndAt()).willReturn(now.plusDays(1).plusHours(8));

        given(substituteRequestRepository.findById(requestId)).willReturn(Optional.of(request));
        given(workplaceRepository.findById(workplaceId)).willReturn(Optional.of(workplace));
        given(substituteCandidateRepository.findByRequestIdAndStatus(
            requestId, CandidateStatus.ACCEPTED))
            .willReturn(Optional.of(candidate));

        given(shiftRepository.existsConflictingShift(
            eq(candidateUserId), any(), any())).willReturn(false);
        given(unavailableTimeRepository.existsOverlappingUnavailableTime(
            eq(candidateUserId), any(), any())).willReturn(false);
        given(substituteCandidateRepository.existsConflictingActiveSubstitute(
            eq(candidateUserId), eq(requestId), any(), any(), any()))
            .willReturn(false);

        // when
        SubstituteRequest result = substituteRequestService.approve(requestId, actorId);

        // then
        assertThat(result).isNotNull();
        verify(workplaceMemberService).requireManager(actorId, workplaceId);
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
        // given
        setupValidRequestFullInfo();
        setupValidCandidateInfo();

        given(shift.getStartAt()).willReturn(now.minusHours(1));

        given(substituteRequestRepository.findById(requestId)).willReturn(Optional.of(request));
        given(workplaceRepository.findById(workplaceId)).willReturn(Optional.of(workplace));
        given(substituteCandidateRepository.findByRequestIdAndStatus(
            requestId, CandidateStatus.ACCEPTED))
            .willReturn(Optional.of(candidate));

        // when & then
        assertThatThrownBy(() -> substituteRequestService.approve(requestId, actorId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(ShiftErrorCode.INVALID_TIME_VALUE.getMessage());
    }

    @Test
    @DisplayName("승인 요청자가 해당 근무지 소속이 아니거나 매니저가 아닌 경우 예외가 발생한다")
    void approve_fail_actor_not_in_workplace() {
        // given
        setupValidRequestBasicInfo();
        given(substituteRequestRepository.findById(requestId)).willReturn(Optional.of(request));

        doThrow(new BusinessException(WorkplaceErrorCode.NOT_WORKPLACE_MEMBER))
            .when(workplaceMemberService).requireManager(actorId, workplaceId);

        // when & then
        assertThatThrownBy(() -> substituteRequestService.approve(requestId, actorId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(WorkplaceErrorCode.NOT_WORKPLACE_MEMBER.getMessage());
    }
    @Test
    @DisplayName("후보자에게 충돌하는 기존 Shift가 존재하는 경우 예외가 발생한다")
    void approve_fail_conflicting_shift() {
        // given
        setupValidRequestFullInfo();
        setupValidCandidateInfo();

        given(shift.getStartAt()).willReturn(now.plusDays(1));
        given(shift.getEndAt()).willReturn(now.plusDays(1).plusHours(8));

        given(substituteRequestRepository.findById(requestId)).willReturn(Optional.of(request));
        given(workplaceRepository.findById(workplaceId)).willReturn(Optional.of(workplace));
        given(substituteCandidateRepository.findByRequestIdAndStatus(
            requestId, CandidateStatus.ACCEPTED))
            .willReturn(Optional.of(candidate));

        given(shiftRepository.existsConflictingShift(
            eq(candidateUserId), any(), any())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> substituteRequestService.approve(requestId, actorId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(SubstituteRequestErrorCode.CONFLICT_SHIFT.getMessage());
    }

    @Test
    @DisplayName("후보자의 불가능한 시간과 중복되는 경우 예외가 발생한다")
    void approve_fail_conflicting_unavailable_time() {
        // given
        setupValidRequestFullInfo();
        setupValidCandidateInfo();

        given(shift.getStartAt()).willReturn(now.plusDays(1));
        given(shift.getEndAt()).willReturn(now.plusDays(1).plusHours(8));

        given(substituteRequestRepository.findById(requestId)).willReturn(Optional.of(request));
        given(workplaceRepository.findById(workplaceId)).willReturn(Optional.of(workplace));
        given(substituteCandidateRepository.findByRequestIdAndStatus(
            requestId, CandidateStatus.ACCEPTED))
            .willReturn(Optional.of(candidate));

        given(shiftRepository.existsConflictingShift(
            eq(candidateUserId), any(), any())).willReturn(false);
        given(unavailableTimeRepository.existsOverlappingUnavailableTime(
            eq(candidateUserId), any(), any())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> substituteRequestService.approve(requestId, actorId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(
                SubstituteRequestErrorCode.CONFLICT_UNAVAILABLE_TIME.getMessage());
    }

    @Test
    @DisplayName("후보자가 다른 활성 대타 약속과 시간 충돌이 있는 경우 예외가 발생한다")
    void approve_fail_conflicting_active_substitute() {
        // given
        setupValidRequestFullInfo();
        setupValidCandidateInfo();

        given(shift.getStartAt()).willReturn(now.plusDays(1));
        given(shift.getEndAt()).willReturn(now.plusDays(1).plusHours(8));

        given(substituteRequestRepository.findById(requestId)).willReturn(Optional.of(request));
        given(workplaceRepository.findById(workplaceId)).willReturn(Optional.of(workplace));
        given(substituteCandidateRepository.findByRequestIdAndStatus(
            requestId, CandidateStatus.ACCEPTED))
            .willReturn(Optional.of(candidate));

        given(shiftRepository.existsConflictingShift(
            eq(candidateUserId), any(), any())).willReturn(false);
        given(unavailableTimeRepository.existsOverlappingUnavailableTime(
            eq(candidateUserId), any(), any())).willReturn(false);
        given(substituteCandidateRepository.existsConflictingActiveSubstitute(
            eq(candidateUserId), eq(requestId), any(), any(), any()))
            .willReturn(true);

        // when & then
        assertThatThrownBy(() -> substituteRequestService.approve(requestId, actorId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(
                SubstituteRequestErrorCode.CONFLICT_ACTIVE_SUBSTITUTE.getMessage());
    }
    @Nested
    @DisplayName("진행 중인 대타 요청 목록 조회 (list)")
    class ListTest {

        @Test
        @DisplayName("성공: OPEN과 ACCEPTED 요청이 섞여있으면, "
            + "ACCEPTED만 acceptedMember가 채워진 DTO 리스트를 반환한다.")
        void list_MixedStatusRequests_ReturnsCorrectDtoList() {
            // given
            given(workplaceRepository.findById(workplaceId)).willReturn(Optional.of(workplace));
            given(workplaceMemberService.requireManager(actorId, workplaceId))
                .willReturn(mock(WorkplaceMember.class));

            // 요청자(requesterMember) 관련 체인 완성
            User requesterUser = mock(User.class);
            given(requesterUser.getName()).willReturn("요청자");
            given(requesterMember.getId()).willReturn(requesterMemberId);
            given(requesterMember.getUser()).willReturn(requesterUser);

            // shift → schedule → workplace 체인 완성 (workplaceId/workplaceName 조회용)
            given(shift.getSchedule()).willReturn(schedule);
            given(schedule.getWorkplace()).willReturn(workplace);
            given(workplace.getId()).willReturn(workplaceId);
            given(workplace.getName()).willReturn("테스트 근무지");
            given(shift.getId()).willReturn(50L);
            given(shift.getStartAt()).willReturn(now.plusDays(1));
            given(shift.getEndAt()).willReturn(now.plusDays(1).plusHours(8));

            // OPEN 요청 - acceptedMember는 null이어야 함
            SubstituteRequest openRequest = mock(SubstituteRequest.class);
            given(openRequest.getId()).willReturn(1L);
            given(openRequest.getStatus()).willReturn(RequestStatus.OPEN);
            given(openRequest.getShift()).willReturn(shift);
            given(openRequest.getRequesterMember()).willReturn(requesterMember);
            given(openRequest.getCreateDate()).willReturn(LocalDateTime.now());

            // ACCEPTED 요청 - acceptedMember가 채워져야 함
            SubstituteRequest acceptedRequest = mock(SubstituteRequest.class);
            given(acceptedRequest.getId()).willReturn(2L);
            given(acceptedRequest.getStatus()).willReturn(RequestStatus.ACCEPTED);
            given(acceptedRequest.getShift()).willReturn(shift);
            given(acceptedRequest.getRequesterMember()).willReturn(requesterMember);
            given(acceptedRequest.getCreateDate()).willReturn(LocalDateTime.now());

            given(substituteRequestRepository.findAllByWorkplaceId(eq(workplaceId), any()))
                .willReturn(List.of(openRequest, acceptedRequest));

            // 수락자(candidate) 관련 체인 완성
            given(candidate.getRequest()).willReturn(acceptedRequest);
            given(candidate.getMember()).willReturn(candidateMember);
            given(candidateMember.getId()).willReturn(candidateUserId);
            given(candidateMember.getUser()).willReturn(candidateUser);
            given(candidateUser.getName()).willReturn("수락자");

            given(substituteCandidateRepository
                .findByRequestIdInAndStatus(List.of(2L), CandidateStatus.ACCEPTED))
                .willReturn(List.of(candidate));

            // when
            List<SubstituteRequestListResponse> result =
                substituteRequestService.list(workplaceId, actorId);

            // then
            assertThat(result).hasSize(2);

            SubstituteRequestListResponse openResponse = result.stream()
                .filter(r -> r.requestId().equals(1L)).findFirst().orElseThrow();
            assertThat(openResponse.acceptedMember()).isNull();

            SubstituteRequestListResponse acceptedResponse = result.stream()
                .filter(r -> r.requestId().equals(2L)).findFirst().orElseThrow();
            assertThat(acceptedResponse.acceptedMember()).isNotNull();
        }

        @Test
        @DisplayName("실패: ACCEPTED 요청인데 수락자가 없으면 예외가 발생한다.")
        void list_AcceptedWithoutCandidate_ThrowsException() {
            // given
            given(workplaceRepository.findById(workplaceId)).willReturn(Optional.of(workplace));
            given(workplaceMemberService.requireManager(actorId, workplaceId))
                .willReturn(mock(WorkplaceMember.class));

            SubstituteRequest acceptedRequest = mock(SubstituteRequest.class);
            given(acceptedRequest.getId()).willReturn(requestId);
            given(acceptedRequest.getStatus()).willReturn(RequestStatus.ACCEPTED);

            given(substituteRequestRepository.findAllByWorkplaceId(eq(workplaceId), any()))
                .willReturn(List.of(acceptedRequest));

            given(substituteCandidateRepository
                .findByRequestIdInAndStatus(List.of(requestId), CandidateStatus.ACCEPTED))
                .willReturn(List.of()); // 수락자 없음

            // when & then
            assertThatThrownBy(() -> substituteRequestService.list(workplaceId, actorId))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("실패: ACCEPTED 요청에 수락자가 2명 이상이면 예외가 발생한다.")
        void list_AcceptedWithDuplicateCandidates_ThrowsException() {
            // given
            given(workplaceRepository.findById(workplaceId)).willReturn(Optional.of(workplace));
            given(workplaceMemberService.requireManager(actorId, workplaceId))
                .willReturn(mock(WorkplaceMember.class));

            SubstituteRequest acceptedRequest = mock(SubstituteRequest.class);
            given(acceptedRequest.getId()).willReturn(requestId);
            given(acceptedRequest.getStatus()).willReturn(RequestStatus.ACCEPTED);

            given(substituteRequestRepository.findAllByWorkplaceId(eq(workplaceId), any()))
                .willReturn(List.of(acceptedRequest));

            SubstituteCandidate candidate2 = mock(SubstituteCandidate.class);
            given(candidate.getRequest()).willReturn(acceptedRequest);
            given(candidate2.getRequest()).willReturn(acceptedRequest);

            given(substituteCandidateRepository
                .findByRequestIdInAndStatus(List.of(requestId), CandidateStatus.ACCEPTED))
                .willReturn(List.of(candidate, candidate2)); // 수락자 2명

            // when & then
            assertThatThrownBy(() -> substituteRequestService.list(workplaceId, actorId))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("실패: 근무지가 존재하지 않으면 "
            + "BusinessException(WORKPLACE_NOT_FOUND)이 발생한다.")
        void list_NotFoundWorkplace_ThrowsException() {
            given(workplaceRepository.findById(workplaceId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> substituteRequestService.list(workplaceId, actorId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(WorkplaceErrorCode.WORKPLACE_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("실패: 매니저 권한이 없으면 예외가 발생한다.")
        void list_NotManager_ThrowsException() {
            given(workplaceRepository.findById(workplaceId)).willReturn(Optional.of(workplace));
            given(workplaceMemberService.requireManager(actorId, workplaceId))
                .willThrow(new BusinessException(WorkplaceErrorCode.MANAGER_REQUIRED));

            assertThatThrownBy(() -> substituteRequestService.list(workplaceId, actorId))
                .isInstanceOf(BusinessException.class);
        }
    }
    @Nested
    @DisplayName("MANAGER 대타 요청 전체 종료 (close)")
    class CloseTest {

        @Test
        @DisplayName("성공: OPEN 상태의 요청을 MANAGER가 종료하면 CLOSED로 변경된다.")
        void close_OpenRequest_Success() {
            // given
            given(substituteRequestRepository.findById(requestId)).willReturn(Optional.of(request));
            given(request.getShift()).willReturn(shift);
            given(shift.getSchedule()).willReturn(schedule);
            given(schedule.getWorkplace()).willReturn(workplace);
            given(workplace.getId()).willReturn(workplaceId);

            given(request.getStatus()).willReturn(RequestStatus.OPEN);
            given(schedule.getStatus()).willReturn(ScheduleStatus.PUBLISHED);
            given(shift.getStatus()).willReturn(ShiftStatus.SCHEDULED);
            given(shift.getStartAt()).willReturn(now.plusDays(1));

            // when
            SubstituteRequest result = substituteRequestService.close(requestId, actorId);

            // then
            assertThat(result).isEqualTo(request);
            verify(workplaceMemberService).requireManager(actorId, workplaceId);
            verify(request).closeByManager(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("성공: ACCEPTED 상태의 요청도 MANAGER가 종료할 수 있다.")
        void close_AcceptedRequest_Success() {
            // given
            given(substituteRequestRepository.findById(requestId)).willReturn(Optional.of(request));
            given(request.getShift()).willReturn(shift);
            given(shift.getSchedule()).willReturn(schedule);
            given(schedule.getWorkplace()).willReturn(workplace);
            given(workplace.getId()).willReturn(workplaceId);

            given(request.getStatus()).willReturn(RequestStatus.ACCEPTED);
            given(schedule.getStatus()).willReturn(ScheduleStatus.PUBLISHED);
            given(shift.getStatus()).willReturn(ShiftStatus.SCHEDULED);
            given(shift.getStartAt()).willReturn(now.plusDays(1));

            // when
            SubstituteRequest result = substituteRequestService.close(requestId, actorId);

            // then
            assertThat(result).isEqualTo(request);
            verify(request).closeByManager(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 요청이면 예외가 발생한다.")
        void close_RequestNotFound_ThrowsException() {
            // given
            given(substituteRequestRepository.findById(requestId))
                .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> substituteRequestService.close(requestId, actorId))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("실패: 실제 Workplace의 MANAGER가 아니면 예외가 발생한다.")
        void close_NotManager_ThrowsException() {
            // given
            given(substituteRequestRepository.findById(requestId)).willReturn(Optional.of(request));
            given(request.getShift()).willReturn(shift);
            given(shift.getSchedule()).willReturn(schedule);
            given(schedule.getWorkplace()).willReturn(workplace);
            given(workplace.getId()).willReturn(workplaceId);

            given(workplaceMemberService.requireManager(actorId, workplaceId))
                .willThrow(new BusinessException(WorkplaceErrorCode.NOT_WORKPLACE_MEMBER));

            // when & then
            assertThatThrownBy(() -> substituteRequestService.close(requestId, actorId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(WorkplaceErrorCode.NOT_WORKPLACE_MEMBER.getMessage());
        }

        @Test
        @DisplayName("실패: 요청이 이미 APPROVED 상태면 예외가 발생한다.")
        void close_AlreadyApproved_ThrowsException() {
            // given
            given(substituteRequestRepository.findById(requestId)).willReturn(Optional.of(request));
            given(request.getShift()).willReturn(shift);
            given(shift.getSchedule()).willReturn(schedule);
            given(schedule.getWorkplace()).willReturn(workplace);
            given(workplace.getId()).willReturn(workplaceId);

            given(request.getStatus()).willReturn(RequestStatus.APPROVED);

            // when & then
            assertThatThrownBy(() -> substituteRequestService.close(requestId, actorId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(SubstituteRequestErrorCode.ALREADY_TERMINATED.getMessage());
        }

        @Test
        @DisplayName("실패: 요청이 이미 CLOSED 상태면 예외가 발생한다.")
        void close_AlreadyClosed_ThrowsException() {
            // given
            given(substituteRequestRepository.findById(requestId)).willReturn(Optional.of(request));
            given(request.getShift()).willReturn(shift);
            given(shift.getSchedule()).willReturn(schedule);
            given(schedule.getWorkplace()).willReturn(workplace);
            given(workplace.getId()).willReturn(workplaceId);

            given(request.getStatus()).willReturn(RequestStatus.CLOSED);

            // when & then
            assertThatThrownBy(() -> substituteRequestService.close(requestId, actorId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(SubstituteRequestErrorCode.ALREADY_TERMINATED.getMessage());
        }

        @Test
        @DisplayName("실패: Schedule이 PUBLISHED 상태가 아니면 예외가 발생한다.")
        void close_ScheduleNotPublished_ThrowsException() {
            // given
            given(substituteRequestRepository.findById(requestId)).willReturn(Optional.of(request));
            given(request.getShift()).willReturn(shift);
            given(shift.getSchedule()).willReturn(schedule);
            given(schedule.getWorkplace()).willReturn(workplace);
            given(workplace.getId()).willReturn(workplaceId);

            given(request.getStatus()).willReturn(RequestStatus.OPEN);
            given(schedule.getStatus()).willReturn(ScheduleStatus.DRAFT);

            // when & then
            assertThatThrownBy(() -> substituteRequestService.close(requestId, actorId))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("실패: Shift가 SCHEDULED 상태가 아니면 예외가 발생한다.")
        void close_ShiftNotScheduled_ThrowsException() {
            // given
            given(substituteRequestRepository.findById(requestId)).willReturn(Optional.of(request));
            given(request.getShift()).willReturn(shift);
            given(shift.getSchedule()).willReturn(schedule);
            given(schedule.getWorkplace()).willReturn(workplace);
            given(workplace.getId()).willReturn(workplaceId);

            given(request.getStatus()).willReturn(RequestStatus.OPEN);
            given(schedule.getStatus()).willReturn(ScheduleStatus.PUBLISHED);
            given(shift.getStatus()).willReturn(ShiftStatus.CANCELLED);

            // when & then
            assertThatThrownBy(() -> substituteRequestService.close(requestId, actorId))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("실패: Shift가 이미 시작된 경우(now >= startAt) 예외가 발생한다.")
        void close_ShiftAlreadyStarted_ThrowsException() {
            // given
            given(substituteRequestRepository.findById(requestId)).willReturn(Optional.of(request));
            given(request.getShift()).willReturn(shift);
            given(shift.getSchedule()).willReturn(schedule);
            given(schedule.getWorkplace()).willReturn(workplace);
            given(workplace.getId()).willReturn(workplaceId);

            given(request.getStatus()).willReturn(RequestStatus.OPEN);
            given(schedule.getStatus()).willReturn(ScheduleStatus.PUBLISHED);
            given(shift.getStatus()).willReturn(ShiftStatus.SCHEDULED);
            given(shift.getStartAt()).willReturn(now.minusMinutes(1)); // 이미 시작됨

            // when & then
            assertThatThrownBy(() -> substituteRequestService.close(requestId, actorId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(SubstituteRequestErrorCode
                    .SHIFT_ALREADY_STARTED.getMessage());
        }

        @Test
        @DisplayName("실패: Shift 시작 시각이 정확히 now와 같으면(경계값) 예외가 발생한다.")
        void close_ShiftStartsExactlyNow_ThrowsException() {
            // given
            given(substituteRequestRepository.findById(requestId))
                .willReturn(Optional.of(request));
            given(request.getShift()).willReturn(shift);
            given(shift.getSchedule()).willReturn(schedule);
            given(schedule.getWorkplace()).willReturn(workplace);
            given(workplace.getId()).willReturn(workplaceId);

            given(request.getStatus()).willReturn(RequestStatus.OPEN);
            given(schedule.getStatus()).willReturn(ScheduleStatus.PUBLISHED);
            given(shift.getStatus()).willReturn(ShiftStatus.SCHEDULED);
            given(shift.getStartAt()).willReturn(now); // startAt == now

            // when & then
            assertThatThrownBy(() -> substituteRequestService.close(requestId, actorId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(SubstituteRequestErrorCode
                    .SHIFT_ALREADY_STARTED.getMessage());
        }

        @Test
        @DisplayName("성공: Candidate 상태는 변경되지 않는다 (요청만 CLOSED 처리).")
        void close_DoesNotModifyCandidateStatus() {
            // given
            given(substituteRequestRepository.findById(requestId))
                .willReturn(Optional.of(request));
            given(request.getShift()).willReturn(shift);
            given(shift.getSchedule()).willReturn(schedule);
            given(schedule.getWorkplace()).willReturn(workplace);
            given(workplace.getId()).willReturn(workplaceId);

            given(request.getStatus()).willReturn(RequestStatus.ACCEPTED);
            given(schedule.getStatus()).willReturn(ScheduleStatus.PUBLISHED);
            given(shift.getStatus()).willReturn(ShiftStatus.SCHEDULED);
            given(shift.getStartAt()).willReturn(now.plusDays(1));

            // when
            substituteRequestService.close(requestId, actorId);

            // then
            // Candidate나 Shift.member를 변경하는 어떤 메서드도 호출되지 않아야 함
            verify(request, never()).approveRequest(any(), any());
            verify(shift, never()).changeMember(any()); // 실제 메서드명에 맞게 조정 필요
        }
    }
}