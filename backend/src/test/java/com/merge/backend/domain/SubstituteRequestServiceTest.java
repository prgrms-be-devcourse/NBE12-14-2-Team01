package com.merge.backend.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import java.util.Map;
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
        @DisplayName("성공: Workplace가 존재하고 매니저 권한이 있으면 대타 요청 목록을 반환한다.")
        void list_Success() {
            // given
            given(workplaceRepository.findById(workplaceId)).willReturn(Optional.of(workplace));
            WorkplaceMember mockManager = mock(WorkplaceMember.class);
            given(workplaceMemberService.requireManager(actorId, workplaceId))
                .willReturn(mockManager);
            given(substituteRequestRepository
                .findAllByWorkplaceId(eq(workplaceId), any(LocalDateTime.class)))
                .willReturn(List.of(request));

            // when
            List<SubstituteRequest> result = substituteRequestService.list(workplaceId, actorId);

            // then
            assertThat(result).containsExactly(request);
            verify(workplaceRepository).findById(workplaceId);
            verify(workplaceMemberService).requireManager(actorId, workplaceId);
            verify(substituteRequestRepository).findAllByWorkplaceId(eq(workplaceId),
                any(LocalDateTime.class));
        }

        @Test
        @DisplayName("실패: 근무지가 존재하지 않으면 "
            + "BusinessException(WORKPLACE_NOT_FOUND)이 발생한다.")
        void list_NotFoundWorkplace_ThrowsException() {
            // given
            given(workplaceRepository.findById(workplaceId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> substituteRequestService.list(workplaceId, actorId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(WorkplaceErrorCode.WORKPLACE_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("실패: 매니저 권한이 없으면 예외가 발생한다.")
        void list_NotManager_ThrowsException() {
            // given
            given(workplaceRepository.findById(workplaceId)).willReturn(Optional.of(workplace));

            // workplaceMemberService에서 권한 검증 실패 예외 발생 설정
            given(workplaceMemberService.requireManager(actorId, workplaceId))
                .willThrow(new BusinessException(WorkplaceErrorCode.MANAGER_REQUIRED));

            // when & then
            assertThatThrownBy(() -> substituteRequestService.list(workplaceId, actorId))
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("수락된 후보자 Map 조회 (getAcceptedCandidates)")
    class GetAcceptedCandidatesTest {

        @Test
        @DisplayName("성공: requestId가 주어지면 candidate가 Map에 담겨 반환된다.")
        void getAcceptedCandidates_WithRequestIds_ReturnsMap() {
            // given
            given(candidate.getRequest()).willReturn(request);
            given(request.getId()).willReturn(requestId);

            given(substituteCandidateRepository
                .findByRequestIdInAndStatus(List.of(requestId), CandidateStatus.ACCEPTED))
                .willReturn(List.of(candidate));

            // when
            Map<Long, SubstituteCandidate> result =
                substituteRequestService.getAcceptedCandidates(List.of(requestId));

            // then
            assertThat(result).containsEntry(requestId, candidate);
            verify(substituteCandidateRepository)
                .findByRequestIdInAndStatus(List.of(requestId), CandidateStatus.ACCEPTED);
        }

        @Test
        @DisplayName("성공: requestId 목록이 비어있으면 조회 쿼리를 호출하지 않고 빈 Map을 반환한다.")
        void getAcceptedCandidates_EmptyRequestIds_ReturnsEmptyMapWithoutQuery() {
            // when
            Map<Long, SubstituteCandidate> result =
                substituteRequestService.getAcceptedCandidates(List.of());

            // then
            assertThat(result).isEmpty();
            verify(substituteCandidateRepository, never())
                .findByRequestIdInAndStatus(anyList(), any());
        }

        @Test
        @DisplayName("성공: requestId가 여러 건이면 IN 절 쿼리 한 번으로 모두 조회된다.")
        void getAcceptedCandidates_MultipleRequestIds_SingleQueryCall() {
            // given
            Long requestId2 = 2L;

            SubstituteRequest req1 = mock(SubstituteRequest.class);
            given(req1.getId()).willReturn(requestId);

            SubstituteRequest req2 = mock(SubstituteRequest.class);
            given(req2.getId()).willReturn(requestId2);

            SubstituteCandidate candidate2 = mock(SubstituteCandidate.class);
            given(candidate.getRequest()).willReturn(req1);
            given(candidate2.getRequest()).willReturn(req2);

            given(substituteCandidateRepository
                .findByRequestIdInAndStatus(
                    List.of(requestId, requestId2), CandidateStatus.ACCEPTED))
                .willReturn(List.of(candidate, candidate2));

            // when
            Map<Long, SubstituteCandidate> result =
                substituteRequestService.getAcceptedCandidates(List.of(requestId, requestId2));

            // then
            assertThat(result)
                .containsEntry(requestId, candidate)
                .containsEntry(requestId2, candidate2);
            verify(substituteCandidateRepository, times(1))
                .findByRequestIdInAndStatus(anyList(), eq(CandidateStatus.ACCEPTED));
        }
    }
}