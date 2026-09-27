package com.merge.backend.domain.substitute.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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
import com.merge.backend.domain.substitute.dto.SentSubstituteRequestResponse;
import com.merge.backend.domain.substitute.dto.SubstituteRequestCreateResponse;
import com.merge.backend.domain.substitute.dto.SubstituteRequestListResponse;
import com.merge.backend.domain.substitute.entity.CandidateStatus;
import com.merge.backend.domain.substitute.entity.RequestCloseReason;
import com.merge.backend.domain.substitute.entity.RequestStatus;
import com.merge.backend.domain.substitute.entity.SubstituteCandidate;
import com.merge.backend.domain.substitute.entity.SubstituteRequest;
import com.merge.backend.domain.substitute.exception.SubstituteRequestErrorCode;
import com.merge.backend.domain.substitute.repository.SubstituteCandidateRepository;
import com.merge.backend.domain.substitute.repository.SubstituteRequestRepository;
import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.workplace.entity.Workplace;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;
import com.merge.backend.domain.workplace.exception.WorkplaceErrorCode;
import com.merge.backend.domain.workplace.repository.WorkplaceRepository;
import com.merge.backend.domain.workplace.service.WorkplaceMemberService;
import com.merge.backend.global.exception.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    private SubstituteRequestService substituteRequestService;

    @Mock
    private SubstituteRequestRepository substituteRequestRepository;

    @Mock
    private SubstituteCandidateRepository substituteCandidateRepository;

    @Mock
    private ShiftRepository shiftRepository;

    @Mock
    private SubstituteCandidateService substituteCandidateService;

    @Mock
    private WorkplaceRepository workplaceRepository;

    @Mock
    private UnavailableTimeRepository unavailableTimeRepository;

    @Mock
    private WorkplaceMemberService workplaceMemberService;

    private final Clock clock = Clock.fixed(
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
        substituteRequestService = new SubstituteRequestService(
            substituteRequestRepository,
            substituteCandidateRepository,
            shiftRepository,
            workplaceRepository,
            unavailableTimeRepository,
            workplaceMemberService,
            clock,
            substituteCandidateService
        );

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

    // ===================== SUB-04 대체 근무 승인 =====================

    @Nested
    @DisplayName("대체 근무 승인 (approve)")
    class ApproveTest {

        @Test
        @DisplayName("대체 근무 승인 성공")
        void approve_success() {
            // given
            setupValidRequestFullInfo();
            setupValidCandidateInfo();

            given(shift.getStartAt()).willReturn(now.plusDays(1));
            given(shift.getEndAt()).willReturn(now.plusDays(1).plusHours(8));

            given(substituteRequestRepository.findById(requestId))
                .willReturn(Optional.of(request));
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
            given(substituteRequestRepository.findById(requestId))
                .willReturn(Optional.of(request));

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

            given(substituteRequestRepository.findById(requestId))
                .willReturn(Optional.of(request));
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
            given(substituteRequestRepository.findById(requestId))
                .willReturn(Optional.of(request));

            given(workplaceMemberService.requireManager(actorId, workplaceId))
                .willThrow(new BusinessException(WorkplaceErrorCode.NOT_WORKPLACE_MEMBER));

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

            given(substituteRequestRepository.findById(requestId))
                .willReturn(Optional.of(request));
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

            given(substituteRequestRepository.findById(requestId))
                .willReturn(Optional.of(request));
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

            given(substituteRequestRepository.findById(requestId))
                .willReturn(Optional.of(request));
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
    }

    // ===================== SUB-06/07 진행 중 요청 목록 조회 =====================

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

            User requesterUser = mock(User.class);
            given(requesterUser.getName()).willReturn("요청자");
            given(requesterMember.getId()).willReturn(requesterMemberId);
            given(requesterMember.getUser()).willReturn(requesterUser);

            given(shift.getSchedule()).willReturn(schedule);
            given(schedule.getWorkplace()).willReturn(workplace);
            given(workplace.getId()).willReturn(workplaceId);
            given(workplace.getName()).willReturn("테스트 근무지");
            given(shift.getId()).willReturn(50L);
            given(shift.getStartAt()).willReturn(now.plusDays(1));
            given(shift.getEndAt()).willReturn(now.plusDays(1).plusHours(8));

            SubstituteRequest openRequest = mock(SubstituteRequest.class);
            given(openRequest.getId()).willReturn(1L);
            given(openRequest.getStatus()).willReturn(RequestStatus.OPEN);
            given(openRequest.getShift()).willReturn(shift);
            given(openRequest.getRequesterMember()).willReturn(requesterMember);
            given(openRequest.getCreateDate()).willReturn(now);

            SubstituteRequest acceptedRequest = mock(SubstituteRequest.class);
            given(acceptedRequest.getId()).willReturn(2L);
            given(acceptedRequest.getStatus()).willReturn(RequestStatus.ACCEPTED);
            given(acceptedRequest.getShift()).willReturn(shift);
            given(acceptedRequest.getRequesterMember()).willReturn(requesterMember);
            given(acceptedRequest.getCreateDate()).willReturn(now);

            given(substituteRequestRepository.findAllByWorkplaceId(eq(workplaceId), any()))
                .willReturn(List.of(openRequest, acceptedRequest));

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
                .willReturn(List.of());

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
                .willReturn(List.of(candidate, candidate2));

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

    // ===================== SUB-08 MANAGER 전체 종료 =====================

    @Nested
    @DisplayName("MANAGER 대타 요청 전체 종료 (close)")
    class CloseTest {

        @Test
        @DisplayName("성공: OPEN 상태의 요청을 MANAGER가 종료하면 CLOSED로 변경된다.")
        void close_OpenRequest_Success() {
            given(substituteRequestRepository.findById(requestId))
                .willReturn(Optional.of(request));
            given(request.getShift()).willReturn(shift);
            given(shift.getSchedule()).willReturn(schedule);
            given(schedule.getWorkplace()).willReturn(workplace);
            given(workplace.getId()).willReturn(workplaceId);

            given(request.getStatus()).willReturn(RequestStatus.OPEN);
            given(schedule.getStatus()).willReturn(ScheduleStatus.PUBLISHED);
            given(shift.getStatus()).willReturn(ShiftStatus.SCHEDULED);
            given(shift.getStartAt()).willReturn(now.plusDays(1));

            SubstituteRequest result = substituteRequestService.close(requestId, actorId);

            assertThat(result).isEqualTo(request);
            verify(workplaceMemberService).requireManager(actorId, workplaceId);
            verify(request).closeByManager(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("성공: ACCEPTED 상태의 요청도 MANAGER가 종료할 수 있다.")
        void close_AcceptedRequest_Success() {
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

            SubstituteRequest result = substituteRequestService.close(requestId, actorId);

            assertThat(result).isEqualTo(request);
            verify(request).closeByManager(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 요청이면 예외가 발생한다.")
        void close_RequestNotFound_ThrowsException() {
            given(substituteRequestRepository.findById(requestId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> substituteRequestService.close(requestId, actorId))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("실패: 실제 Workplace의 MANAGER가 아니면 예외가 발생한다.")
        void close_NotManager_ThrowsException() {
            given(substituteRequestRepository.findById(requestId))
                .willReturn(Optional.of(request));
            given(request.getShift()).willReturn(shift);
            given(shift.getSchedule()).willReturn(schedule);
            given(schedule.getWorkplace()).willReturn(workplace);
            given(workplace.getId()).willReturn(workplaceId);

            given(workplaceMemberService.requireManager(actorId, workplaceId))
                .willThrow(new BusinessException(WorkplaceErrorCode.NOT_WORKPLACE_MEMBER));

            assertThatThrownBy(() -> substituteRequestService.close(requestId, actorId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(WorkplaceErrorCode.NOT_WORKPLACE_MEMBER.getMessage());
        }

        @Test
        @DisplayName("실패: 요청이 이미 APPROVED 상태면 예외가 발생한다.")
        void close_AlreadyApproved_ThrowsException() {
            given(substituteRequestRepository.findById(requestId))
                .willReturn(Optional.of(request));
            given(request.getShift()).willReturn(shift);
            given(shift.getSchedule()).willReturn(schedule);
            given(schedule.getWorkplace()).willReturn(workplace);
            given(workplace.getId()).willReturn(workplaceId);

            given(request.getStatus()).willReturn(RequestStatus.APPROVED);

            assertThatThrownBy(() -> substituteRequestService.close(requestId, actorId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(SubstituteRequestErrorCode.ALREADY_TERMINATED.getMessage());
        }

        @Test
        @DisplayName("실패: 요청이 이미 CLOSED 상태면 예외가 발생한다.")
        void close_AlreadyClosed_ThrowsException() {
            given(substituteRequestRepository.findById(requestId))
                .willReturn(Optional.of(request));
            given(request.getShift()).willReturn(shift);
            given(shift.getSchedule()).willReturn(schedule);
            given(schedule.getWorkplace()).willReturn(workplace);
            given(workplace.getId()).willReturn(workplaceId);

            given(request.getStatus()).willReturn(RequestStatus.CLOSED);

            assertThatThrownBy(() -> substituteRequestService.close(requestId, actorId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(SubstituteRequestErrorCode.ALREADY_TERMINATED.getMessage());
        }

        @Test
        @DisplayName("실패: Schedule이 PUBLISHED 상태가 아니면 예외가 발생한다.")
        void close_ScheduleNotPublished_ThrowsException() {
            given(substituteRequestRepository.findById(requestId))
                .willReturn(Optional.of(request));
            given(request.getShift()).willReturn(shift);
            given(shift.getSchedule()).willReturn(schedule);
            given(schedule.getWorkplace()).willReturn(workplace);
            given(workplace.getId()).willReturn(workplaceId);

            given(request.getStatus()).willReturn(RequestStatus.OPEN);
            given(schedule.getStatus()).willReturn(ScheduleStatus.DRAFT);

            assertThatThrownBy(() -> substituteRequestService.close(requestId, actorId))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("실패: Shift가 SCHEDULED 상태가 아니면 예외가 발생한다.")
        void close_ShiftNotScheduled_ThrowsException() {
            given(substituteRequestRepository.findById(requestId))
                .willReturn(Optional.of(request));
            given(request.getShift()).willReturn(shift);
            given(shift.getSchedule()).willReturn(schedule);
            given(schedule.getWorkplace()).willReturn(workplace);
            given(workplace.getId()).willReturn(workplaceId);

            given(request.getStatus()).willReturn(RequestStatus.OPEN);
            given(schedule.getStatus()).willReturn(ScheduleStatus.PUBLISHED);
            given(shift.getStatus()).willReturn(ShiftStatus.CANCELLED);

            assertThatThrownBy(() -> substituteRequestService.close(requestId, actorId))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("실패: Shift가 이미 시작된 경우(now >= startAt) 예외가 발생한다.")
        void close_ShiftAlreadyStarted_ThrowsException() {
            given(substituteRequestRepository.findById(requestId))
                .willReturn(Optional.of(request));
            given(request.getShift()).willReturn(shift);
            given(shift.getSchedule()).willReturn(schedule);
            given(schedule.getWorkplace()).willReturn(workplace);
            given(workplace.getId()).willReturn(workplaceId);

            given(request.getStatus()).willReturn(RequestStatus.OPEN);
            given(schedule.getStatus()).willReturn(ScheduleStatus.PUBLISHED);
            given(shift.getStatus()).willReturn(ShiftStatus.SCHEDULED);
            given(shift.getStartAt()).willReturn(now.minusMinutes(1));

            assertThatThrownBy(() -> substituteRequestService.close(requestId, actorId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(SubstituteRequestErrorCode
                    .SHIFT_ALREADY_STARTED_NOT_CLOSE.getMessage());
        }

        @Test
        @DisplayName("실패: Shift 시작 시각이 정확히 now와 같으면(경계값) 예외가 발생한다.")
        void close_ShiftStartsExactlyNow_ThrowsException() {
            given(substituteRequestRepository.findById(requestId))
                .willReturn(Optional.of(request));
            given(request.getShift()).willReturn(shift);
            given(shift.getSchedule()).willReturn(schedule);
            given(schedule.getWorkplace()).willReturn(workplace);
            given(workplace.getId()).willReturn(workplaceId);

            given(request.getStatus()).willReturn(RequestStatus.OPEN);
            given(schedule.getStatus()).willReturn(ScheduleStatus.PUBLISHED);
            given(shift.getStatus()).willReturn(ShiftStatus.SCHEDULED);
            given(shift.getStartAt()).willReturn(now);

            assertThatThrownBy(() -> substituteRequestService.close(requestId, actorId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(SubstituteRequestErrorCode
                    .SHIFT_ALREADY_STARTED_NOT_CLOSE.getMessage());
        }

        @Test
        @DisplayName("성공: Candidate 상태는 변경되지 않는다 (요청만 CLOSED 처리).")
        void close_DoesNotModifyCandidateStatus() {
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

            substituteRequestService.close(requestId, actorId);

            verify(request, never()).approveRequest(any(), any());
            verify(shift, never()).changeMember(any()); // 실제 메서드명에 맞게 조정 필요
        }
    }

    // ===================== SUB-01 대타 요청 생성 =====================

    @Nested
    @DisplayName("대타 요청 생성 (create)")
    class CreateTest {

        @Test
        @DisplayName("SUB-01 - 검증을 통과하면 대타 요청이 OPEN으로 저장된다")
        void create_Success_SavesAsOpen() {
            Shift newShift = createShift(
                publishedSchedule(), REQUESTER_USER_ID, futureStartAt(), ShiftStatus.SCHEDULED
            );

            WorkplaceMember newCandidateMember = mock(WorkplaceMember.class);

            given(shiftRepository.findById(SHIFT_ID)).willReturn(Optional.of(newShift));
            given(substituteRequestRepository
                .existsByShift_IdAndStatusIn(SHIFT_ID, ACTIVE_STATUSES))
                .willReturn(false);

            given(substituteCandidateService.findCandidates(
                newShift.getSchedule().getWorkplace().getId(),
                newShift.getMember().getId(),
                newShift
            )).willReturn(List.of(newCandidateMember));

            given(substituteRequestRepository.save(any(SubstituteRequest.class)))
                .willAnswer(invocation -> {
                    SubstituteRequest saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", SAVED_REQUEST_ID);
                    return saved;
                });

            given(substituteCandidateService.createCandidates(
                any(SubstituteRequest.class), any()))
                .willReturn(List.of(mock(SubstituteCandidate.class)));

            SubstituteRequestCreateResponse response =
                substituteRequestService.create(SHIFT_ID, REQUESTER_USER_ID);

            ArgumentCaptor<SubstituteRequest> captor =
                ArgumentCaptor.forClass(SubstituteRequest.class);
            verify(substituteRequestRepository).save(captor.capture());
            SubstituteRequest saved = captor.getValue();
            assertThat(saved.getShift()).isSameAs(newShift);
            assertThat(saved.getRequesterMember()).isSameAs(newShift.getMember());
            assertThat(saved.getStatus()).isEqualTo(RequestStatus.OPEN);

            assertThat(response.requestCreated()).isTrue();
            assertThat(response.requestId()).isEqualTo(SAVED_REQUEST_ID);
            assertThat(response.shiftId()).isEqualTo(SHIFT_ID);
            assertThat(response.status()).isEqualTo(RequestStatus.OPEN);
            assertThat(response.candidateCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("SUB-01 - 대타 후보가 없으면 Request를 생성하지 않는다")
        void create_NoCandidate_DoesNotCreateRequest() {
            Shift newShift = createShift(
                publishedSchedule(), REQUESTER_USER_ID, futureStartAt(), ShiftStatus.SCHEDULED
            );

            given(shiftRepository.findById(SHIFT_ID)).willReturn(Optional.of(newShift));
            given(substituteRequestRepository
                .existsByShift_IdAndStatusIn(SHIFT_ID, ACTIVE_STATUSES))
                .willReturn(false);

            given(substituteCandidateService.findCandidates(
                newShift.getSchedule().getWorkplace().getId(),
                newShift.getMember().getId(),
                newShift
            )).willReturn(List.of());

            SubstituteRequestCreateResponse response =
                substituteRequestService.create(SHIFT_ID, REQUESTER_USER_ID);

            assertThat(response.requestCreated()).isFalse();
            assertThat(response.requestId()).isNull();
            assertThat(response.shiftId()).isEqualTo(SHIFT_ID);
            assertThat(response.status()).isNull();
            assertThat(response.candidateCount()).isZero();

            verify(substituteRequestRepository, never()).save(any(SubstituteRequest.class));
            verify(substituteCandidateService, never())
                .createCandidates(any(SubstituteRequest.class), any());
        }

        @Test
        @DisplayName("SUB-01 실패 - 존재하지 않는 Shift")
        void create_Fail_ShiftNotFound() {
            given(shiftRepository.findById(SHIFT_ID)).willReturn(Optional.empty());

            assertCreateFails(SubstituteRequestErrorCode.SHIFT_NOT_FOUND);
        }

        @Test
        @DisplayName("SUB-01 실패 - 공개되지 않은 근무표의 Shift")
        void create_Fail_ScheduleNotPublished() {
            Shift newShift = createShift(
                draftSchedule(), REQUESTER_USER_ID, futureStartAt(), ShiftStatus.SCHEDULED
            );
            given(shiftRepository.findById(SHIFT_ID)).willReturn(Optional.of(newShift));

            assertCreateFails(SubstituteRequestErrorCode.SHIFT_NOT_PUBLISHED);
        }

        @Test
        @DisplayName("SUB-01 실패 - 취소된 Shift")
        void create_Fail_ShiftCancelled() {
            Shift newShift = createShift(
                publishedSchedule(), REQUESTER_USER_ID, futureStartAt(), ShiftStatus.CANCELLED
            );
            given(shiftRepository.findById(SHIFT_ID)).willReturn(Optional.of(newShift));

            assertCreateFails(SubstituteRequestErrorCode.SHIFT_CANCELLED);
        }

        @Test
        @DisplayName("SUB-01 실패 - 본인의 Shift가 아님")
        void create_Fail_NotOwnShift() {
            Shift newShift = createShift(
                publishedSchedule(), OTHER_USER_ID, futureStartAt(), ShiftStatus.SCHEDULED
            );
            given(shiftRepository.findById(SHIFT_ID)).willReturn(Optional.of(newShift));

            assertCreateFails(SubstituteRequestErrorCode.NOT_OWN_SHIFT);
        }

        @Test
        @DisplayName("SUB-01 실패 - 이미 시작된 Shift")
        void create_Fail_ShiftAlreadyStarted() {
            Shift newShift = createShift(
                publishedSchedule(),
                REQUESTER_USER_ID,
                now.minusMinutes(1),
                ShiftStatus.SCHEDULED
            );
            given(shiftRepository.findById(SHIFT_ID)).willReturn(Optional.of(newShift));

            assertCreateFails(SubstituteRequestErrorCode.SHIFT_ALREADY_STARTED);
        }

        @Test
        @DisplayName("SUB-01 실패 - 이미 진행 중인(OPEN/ACCEPTED) 요청이 있음")
        void create_Fail_ActiveRequestExists() {
            Shift newShift = createShift(
                publishedSchedule(), REQUESTER_USER_ID, futureStartAt(), ShiftStatus.SCHEDULED
            );
            given(shiftRepository.findById(SHIFT_ID)).willReturn(Optional.of(newShift));
            given(substituteRequestRepository
                .existsByShift_IdAndStatusIn(SHIFT_ID, ACTIVE_STATUSES))
                .willReturn(true);

            assertCreateFails(SubstituteRequestErrorCode.ACTIVE_REQUEST_EXISTS);
        }

        private void assertCreateFails(SubstituteRequestErrorCode expected) {
            assertThatThrownBy(() -> substituteRequestService.create(SHIFT_ID, REQUESTER_USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(expected);

            verify(substituteRequestRepository, never()).save(any(SubstituteRequest.class));
        }
    }

    // ===================== SUB-05 보낸 요청 목록 조회 =====================

    @Nested
    @DisplayName("보낸 대타 요청 목록 조회 (getSentRequests)")
    class SentRequestsTest {

        @Test
        @DisplayName("SUB-05 - 보낸 요청 목록을 최신순으로 반환하고, "
            + "만료된 요청은 CLOSED/EXPIRED로 표시된다")
        void getSentRequests_ReturnsLatestFirst_MarksExpired() {
            Shift openShift = createShift(
                publishedSchedule(), REQUESTER_USER_ID, futureStartAt(), ShiftStatus.SCHEDULED
            );
            SubstituteRequest openRequest =
                new SubstituteRequest(openShift, openShift.getMember(), RequestStatus.OPEN);
            ReflectionTestUtils.setField(openRequest, "id", 1L);
            ReflectionTestUtils.setField(openRequest, "createDate", now.minusDays(1));

            Shift expiredShift = createShift(
                publishedSchedule(),
                REQUESTER_USER_ID,
                now.minusMinutes(1),
                ShiftStatus.SCHEDULED
            );
            SubstituteRequest expiredRequest =
                new SubstituteRequest(expiredShift, expiredShift.getMember(), RequestStatus.OPEN);
            ReflectionTestUtils.setField(expiredRequest, "id", 2L);
            ReflectionTestUtils.setField(expiredRequest, "createDate", now);

            given(substituteRequestRepository
                .findAllByRequesterMember_User_IdOrderByCreateDateDesc(REQUESTER_USER_ID))
                .willReturn(List.of(expiredRequest, openRequest));

            List<SentSubstituteRequestResponse> responses =
                substituteRequestService.getSentRequests(REQUESTER_USER_ID);

            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).requestId()).isEqualTo(2L);
            assertThat(responses.get(0).status()).isEqualTo(RequestStatus.CLOSED);
            assertThat(responses.get(0).closeReason()).isEqualTo(RequestCloseReason.EXPIRED);
            assertThat(responses.get(1).requestId()).isEqualTo(1L);
            assertThat(responses.get(1).status()).isEqualTo(RequestStatus.OPEN);
        }

        @Test
        @DisplayName("SUB-05 - 보낸 요청이 없으면 빈 리스트를 반환한다")
        void getSentRequests_NoRequests_ReturnsEmptyList() {
            given(substituteRequestRepository
                .findAllByRequesterMember_User_IdOrderByCreateDateDesc(REQUESTER_USER_ID))
                .willReturn(List.of());

            List<SentSubstituteRequestResponse> responses =
                substituteRequestService.getSentRequests(REQUESTER_USER_ID);

            assertThat(responses).isEmpty();
        }
    }

    // ===================== 공용 fixture 헬퍼 =====================

    private Shift createShift(
        Schedule scheduleFixture,
        Long ownerUserId,
        LocalDateTime startAt,
        ShiftStatus status
    ) {
        WorkplaceMember owner = new WorkplaceMember(
            scheduleFixture.getWorkplace(),
            new User(ownerUserId, "owner@example.com", "담당자"),
            WorkplaceRole.EMPLOYEE,
            now.minusDays(30),
            null
        );

        return new Shift(scheduleFixture, owner, startAt, startAt.plusHours(4), status);
    }

    private Schedule draftSchedule() {
        return new Schedule(new Workplace("SWITCH 카페", "INVITE01"), LocalDate.now(clock));
    }

    private Schedule publishedSchedule() {
        Schedule scheduleFixture = draftSchedule();
        scheduleFixture.publish(now);
        return scheduleFixture;
    }

    private LocalDateTime futureStartAt() {
        return now.plusDays(1);
    }
}