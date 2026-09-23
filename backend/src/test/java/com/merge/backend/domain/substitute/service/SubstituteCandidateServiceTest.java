package com.merge.backend.domain.substitute.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.merge.backend.domain.shift.entity.Schedule;
import com.merge.backend.domain.shift.entity.ScheduleStatus;
import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.entity.ShiftStatus;
import com.merge.backend.domain.shift.repository.ShiftRepository;
import com.merge.backend.domain.shift.repository.UnavailableTimeRepository;
import com.merge.backend.domain.substitute.entity.CandidateStatus;
import com.merge.backend.domain.substitute.entity.RequestStatus;
import com.merge.backend.domain.substitute.entity.SubstituteCandidate;
import com.merge.backend.domain.substitute.entity.SubstituteRequest;
import com.merge.backend.domain.substitute.repository.SubstituteCandidateRepository;
import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;
import com.merge.backend.domain.workplace.repository.WorkplaceMemberRepository;
import com.merge.backend.global.exception.BusinessException;
import java.time.Clock;
import java.util.Optional;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubstituteCandidateServiceTest {

    @Mock
    private WorkplaceMemberRepository workplaceMemberRepository;

    @Mock
    private ShiftRepository shiftRepository;

    @Mock
    private UnavailableTimeRepository unavailableTimeRepository;

    @Mock
    private SubstituteCandidateRepository substituteCandidateRepository;

    @InjectMocks
    private SubstituteCandidateService substituteCandidateService;

    @Mock
    private Clock clock; // 테스트에서 사용할 가짜 시간

    @Mock
    private SubstituteRequest substituteRequest;

    @Mock
    private Shift shift;

    @Mock
    private Schedule schedule;

    @Mock
    private WorkplaceMember member;

    @Mock
    private User user;

    @Mock
    private SubstituteCandidate candidate;

    @Test
    @DisplayName("SUB-02 - 현재 사용자에게 온 응답 가능한 대타 요청을 조회한다")
    void findReceivedRequests_Success() {

        // given
        Long userId = 1L; // 현재 로그인한 사용자

        SubstituteCandidate candidate1 =
            mock(SubstituteCandidate.class); // 조회될 대타 후보 1

        SubstituteCandidate candidate2 =
            mock(SubstituteCandidate.class); // 조회될 대타 후보 2

        List<SubstituteCandidate> expected =
            List.of(candidate1, candidate2); // Repository가 돌려줄 결과

        when(clock.instant())
            .thenReturn(Instant.parse("2026-09-22T06:00:00Z"));

        when(clock.getZone())
            .thenReturn(ZoneId.of("Asia/Seoul"));

        when(
            substituteCandidateRepository.findReceivedRequests(
                eq(userId),
                any(LocalDateTime.class)
            )
        ).thenReturn(expected);

        // when
        List<SubstituteCandidate> result =
            substituteCandidateService.findReceivedRequests(userId);

        // then
        assertThat(result)
            .containsExactly(candidate1, candidate2);

        verify(substituteCandidateRepository)
            .findReceivedRequests(
                eq(userId),
                any(LocalDateTime.class)
            );
    }


    @Test
    @DisplayName("SUB-03 - 대타 요청을 수락한다")
    void acceptCandidate_Success() {

        // given
        Long candidateId = 1L;
        Long userId = 10L;

        LocalDateTime startAt =
            LocalDateTime.of(2026, 9, 23, 18, 0);

        LocalDateTime endAt =
            LocalDateTime.of(2026, 9, 23, 22, 0);

        when(clock.instant())
            .thenReturn(Instant.parse("2026-09-22T06:00:00Z"));

        when(clock.getZone())
            .thenReturn(ZoneId.of("Asia/Seoul"));

        // Candidate 조회
        when(substituteCandidateRepository.findById(candidateId))
            .thenReturn(Optional.of(candidate));

        // Candidate → Member → User
        when(candidate.getMember()).thenReturn(member);
        when(member.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(userId);

        // 현재도 유효한 EMPLOYEE
        when(member.getLeftAt()).thenReturn(null);
        when(member.getRole()).thenReturn(WorkplaceRole.EMPLOYEE);

        // 아직 응답 전
        when(candidate.getStatus()).thenReturn(CandidateStatus.PENDING);

        // Candidate → Request
        when(candidate.getRequest()).thenReturn(substituteRequest);
        when(substituteRequest.getStatus()).thenReturn(RequestStatus.OPEN);

        // Request → Shift
        when(substituteRequest.getShift()).thenReturn(shift);

        when(shift.getSchedule()).thenReturn(schedule);
        when(schedule.getStatus()).thenReturn(ScheduleStatus.PUBLISHED);

        when(shift.getStatus()).thenReturn(ShiftStatus.SCHEDULED);
        when(shift.getStartAt()).thenReturn(startAt);
        when(shift.getEndAt()).thenReturn(endAt);
        when(shift.getId()).thenReturn(100L);

        // 최신 충돌 없음
        when(shiftRepository.existsOverlappingOfficialShift(
            userId,
            ScheduleStatus.PUBLISHED,
            startAt,
            endAt,
            100L
        )).thenReturn(false);

        when(unavailableTimeRepository.existsOverlappingUnavailableTime(
            userId,
            startAt,
            endAt
        )).thenReturn(false);

        when(substituteCandidateRepository.existsOverlappingAcceptedSubstitute(
            userId,
            startAt,
            endAt
        )).thenReturn(false);

        // when
        SubstituteCandidate result =
            substituteCandidateService.acceptCandidate(candidateId, userId);

        // then
        assertThat(result).isEqualTo(candidate);

        verify(candidate).accept(any(LocalDateTime.class));
        verify(substituteRequest).accept();
    }

    @Test
    @DisplayName("SUB-03 - 대타 요청을 거절하고 다른 후보가 남아있으면 Request는 OPEN을 유지한다")
    void rejectCandidate_Success_OtherPendingCandidateExists() {

        // given
        Long candidateId = 1L;
        Long requestId = 50L;
        Long userId = 10L;

        LocalDateTime startAt =
            LocalDateTime.of(2026, 9, 23, 18, 0);

        // 현재 시간 고정
        when(clock.instant())
            .thenReturn(Instant.parse("2026-09-22T06:00:00Z"));

        when(clock.getZone())
            .thenReturn(ZoneId.of("Asia/Seoul"));

        // Candidate 조회
        when(substituteCandidateRepository.findById(candidateId))
            .thenReturn(Optional.of(candidate));

        // Candidate → Member → User
        when(candidate.getMember()).thenReturn(member);
        when(member.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(userId);

        // 현재도 유효한 EMPLOYEE
        when(member.getLeftAt()).thenReturn(null);
        when(member.getRole()).thenReturn(WorkplaceRole.EMPLOYEE);

        // 아직 응답 전
        when(candidate.getStatus()).thenReturn(CandidateStatus.PENDING);

        // Candidate → Request
        when(candidate.getRequest()).thenReturn(substituteRequest);
        when(candidate.getId()).thenReturn(candidateId);

        when(substituteRequest.getId()).thenReturn(requestId);
        when(substituteRequest.getStatus()).thenReturn(RequestStatus.OPEN);

        // Request → Shift
        when(substituteRequest.getShift()).thenReturn(shift);

        when(shift.getSchedule()).thenReturn(schedule);
        when(schedule.getStatus()).thenReturn(ScheduleStatus.PUBLISHED);

        when(shift.getStatus()).thenReturn(ShiftStatus.SCHEDULED);
        when(shift.getStartAt()).thenReturn(startAt);

        // 나 말고 다른 PENDING 후보가 남아 있음
        when(
            substituteCandidateRepository.existsByRequest_IdAndStatusAndIdNot(
                requestId,
                CandidateStatus.PENDING,
                candidateId
            )
        ).thenReturn(true);

        // when
        SubstituteCandidate result =
            substituteCandidateService.rejectCandidate(candidateId, userId);

        // then
        assertThat(result).isEqualTo(candidate);

        // 현재 Candidate는 거절 처리됨
        verify(candidate).reject(any(LocalDateTime.class));

        // 다른 후보가 남아 있으므로 Request는 CLOSED 되지 않음
        verify(substituteRequest, never())
            .closeAllCandidatesRejected(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("SUB-03 - 마지막 후보가 거절하면 Request를 CLOSED 처리한다")
    void rejectCandidate_LastCandidate_ClosesRequest() {

        // given
        Long candidateId = 1L;
        Long requestId = 50L;
        Long userId = 10L;

        LocalDateTime startAt =
            LocalDateTime.of(2026, 9, 23, 18, 0);

        when(clock.instant())
            .thenReturn(Instant.parse("2026-09-22T06:00:00Z"));

        when(clock.getZone())
            .thenReturn(ZoneId.of("Asia/Seoul"));

        // Candidate 조회
        when(substituteCandidateRepository.findById(candidateId))
            .thenReturn(Optional.of(candidate));

        // Candidate → Member → User
        when(candidate.getMember()).thenReturn(member);
        when(member.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(userId);

        // 현재도 유효한 EMPLOYEE
        when(member.getLeftAt()).thenReturn(null);
        when(member.getRole()).thenReturn(WorkplaceRole.EMPLOYEE);

        // 아직 응답 전
        when(candidate.getStatus()).thenReturn(CandidateStatus.PENDING);

        // Candidate → Request
        when(candidate.getRequest()).thenReturn(substituteRequest);
        when(candidate.getId()).thenReturn(candidateId);

        when(substituteRequest.getId()).thenReturn(requestId);
        when(substituteRequest.getStatus()).thenReturn(RequestStatus.OPEN);

        // Request → Shift
        when(substituteRequest.getShift()).thenReturn(shift);

        when(shift.getSchedule()).thenReturn(schedule);
        when(schedule.getStatus()).thenReturn(ScheduleStatus.PUBLISHED);

        when(shift.getStatus()).thenReturn(ShiftStatus.SCHEDULED);
        when(shift.getStartAt()).thenReturn(startAt);

        // 나 말고 다른 PENDING 후보가 없음
        when(
            substituteCandidateRepository.existsByRequest_IdAndStatusAndIdNot(
                requestId,
                CandidateStatus.PENDING,
                candidateId
            )
        ).thenReturn(false);

        // when
        SubstituteCandidate result =
            substituteCandidateService.rejectCandidate(candidateId, userId);

        // then
        assertThat(result).isEqualTo(candidate);

        // Candidate는 거절 처리
        verify(candidate).reject(any(LocalDateTime.class));

        // 마지막 후보이므로 Request도 종료 처리
        verify(substituteRequest)
            .closeAllCandidatesRejected(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("SUB-03 - 이미 응답한 Candidate는 다시 수락할 수 없다")
    void acceptCandidate_AlreadyResponded_Fail() {

        // given
        Long candidateId = 1L;
        Long userId = 10L;

        when(clock.instant())
            .thenReturn(Instant.parse("2026-09-22T06:00:00Z"));

        when(clock.getZone())
            .thenReturn(ZoneId.of("Asia/Seoul"));

        // Candidate 조회
        when(substituteCandidateRepository.findById(candidateId))
            .thenReturn(Optional.of(candidate));

        // Candidate → Member → User
        when(candidate.getMember()).thenReturn(member);
        when(member.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(userId);

        // 현재도 유효한 EMPLOYEE
        when(member.getLeftAt()).thenReturn(null);
        when(member.getRole()).thenReturn(WorkplaceRole.EMPLOYEE);

        // 이미 수락한 Candidate
        when(candidate.getStatus()).thenReturn(CandidateStatus.ACCEPTED);

        // when & then
        assertThrows(
            BusinessException.class,
            () -> substituteCandidateService.acceptCandidate(candidateId, userId)
        );

        // 이미 응답했으므로 다시 accept 처리되면 안 됨
        verify(candidate, never())
            .accept(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("SUB-03 - 공식 근무와 시간이 겹치면 대타 요청을 수락할 수 없다")
    void acceptCandidate_OfficialShiftConflict_Fail() {

        // given
        Long candidateId = 1L;
        Long userId = 10L;
        Long shiftId = 100L;

        LocalDateTime startAt =
            LocalDateTime.of(2026, 9, 23, 18, 0);

        LocalDateTime endAt =
            LocalDateTime.of(2026, 9, 23, 22, 0);

        when(clock.instant())
            .thenReturn(Instant.parse("2026-09-22T06:00:00Z"));

        when(clock.getZone())
            .thenReturn(ZoneId.of("Asia/Seoul"));

        // Candidate 조회
        when(substituteCandidateRepository.findById(candidateId))
            .thenReturn(Optional.of(candidate));

        // Candidate → Member → User
        when(candidate.getMember()).thenReturn(member);
        when(member.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(userId);

        // 현재도 유효한 EMPLOYEE
        when(member.getLeftAt()).thenReturn(null);
        when(member.getRole()).thenReturn(WorkplaceRole.EMPLOYEE);

        // 아직 응답 전
        when(candidate.getStatus()).thenReturn(CandidateStatus.PENDING);

        // Candidate → Request
        when(candidate.getRequest()).thenReturn(substituteRequest);
        when(substituteRequest.getStatus()).thenReturn(RequestStatus.OPEN);

        // Request → Shift
        when(substituteRequest.getShift()).thenReturn(shift);

        when(shift.getSchedule()).thenReturn(schedule);
        when(schedule.getStatus()).thenReturn(ScheduleStatus.PUBLISHED);

        when(shift.getStatus()).thenReturn(ShiftStatus.SCHEDULED);
        when(shift.getStartAt()).thenReturn(startAt);
        when(shift.getEndAt()).thenReturn(endAt);
        when(shift.getId()).thenReturn(shiftId);

        // 겹치는 공식 근무가 있음
        when(shiftRepository.existsOverlappingOfficialShift(
            userId,
            ScheduleStatus.PUBLISHED,
            startAt,
            endAt,
            shiftId
        )).thenReturn(true);

        // when & then
        assertThrows(
            BusinessException.class,
            () -> substituteCandidateService.acceptCandidate(candidateId, userId)
        );

        // 충돌이 있으므로 상태 변경이 일어나면 안 됨
        verify(candidate, never())
            .accept(any(LocalDateTime.class));

        verify(substituteRequest, never())
            .accept();
    }
}