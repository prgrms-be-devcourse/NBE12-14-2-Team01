package com.merge.backend.domain.substitute.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.merge.backend.domain.shift.entity.Schedule;
import com.merge.backend.domain.shift.entity.ScheduleStatus;
import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.entity.ShiftStatus;
import com.merge.backend.domain.substitute.entity.CandidateStatus;
import com.merge.backend.domain.substitute.entity.RequestStatus;
import com.merge.backend.domain.substitute.entity.SubstituteCandidate;
import com.merge.backend.domain.substitute.entity.SubstituteRequest;
import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.workplace.entity.Workplace;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;
import com.merge.backend.global.config.TimeConfig;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import(TimeConfig.class)
class SubstituteCandidateRepositoryTest {

    private final SubstituteCandidateRepository substituteCandidateRepository;
    private final TestEntityManager entityManager;

    @Autowired
    SubstituteCandidateRepositoryTest(
        SubstituteCandidateRepository substituteCandidateRepository,
        TestEntityManager entityManager
    ) {
        this.substituteCandidateRepository = substituteCandidateRepository;
        this.entityManager = entityManager;
    }

    @Test
    @DisplayName("SUB-02 - 지금 응답 가능한 대타 요청만 조회한다")
    void findReceivedRequests_Success() {

        // given
        LocalDateTime now = LocalDateTime.of(2026, 9, 21, 12, 0);

        Workplace workplace = entityManager.persist(
            new Workplace("SWITCH 카페", "TEST1234")
        );

        User requesterUser = entityManager.persist(
            new User("requester@test.com", "password", "요청자")
        );

        User candidateUser = entityManager.persist(
            new User("candidate@test.com", "password", "후보자")
        );

        WorkplaceMember requesterMember = entityManager.persist(
            new WorkplaceMember(
                workplace,
                requesterUser,
                WorkplaceRole.EMPLOYEE,
                now.minusMonths(1)
            )
        );

        WorkplaceMember candidateMember = entityManager.persist(
            new WorkplaceMember(
                workplace,
                candidateUser,
                WorkplaceRole.EMPLOYEE,
                now.minusMonths(1)
            )
        );

        // 공개된 공식 근무표
        Schedule publishedSchedule =
            new Schedule(workplace, LocalDate.of(2026, 9, 21));

        publishedSchedule.publish(now.minusDays(1));
        entityManager.persist(publishedSchedule);

        // 아직 공개되지 않은 근무표
        Schedule draftSchedule =
            new Schedule(workplace, LocalDate.of(2026, 9, 28));

        entityManager.persist(draftSchedule);

        // 1. 모든 조건을 만족하는 정상 Candidate
        SubstituteCandidate validCandidate = createCandidate(
            publishedSchedule,
            requesterMember,
            candidateMember,
            now.plusHours(2),      // 미래 근무
            ShiftStatus.SCHEDULED,
            RequestStatus.OPEN,
            CandidateStatus.PENDING
        );

        // 2. 이미 거절한 Candidate
        createCandidate(
            publishedSchedule,
            requesterMember,
            candidateMember,
            now.plusHours(3),
            ShiftStatus.SCHEDULED,
            RequestStatus.OPEN,
            CandidateStatus.REJECTED
        );

        // 3. 이미 닫힌 Request
        createCandidate(
            publishedSchedule,
            requesterMember,
            candidateMember,
            now.plusHours(4),
            ShiftStatus.SCHEDULED,
            RequestStatus.CLOSED,
            CandidateStatus.PENDING
        );

        // 4. 이미 시작 시간이 지난 Shift
        createCandidate(
            publishedSchedule,
            requesterMember,
            candidateMember,
            now.minusHours(5),
            ShiftStatus.SCHEDULED,
            RequestStatus.OPEN,
            CandidateStatus.PENDING
        );

        // 5. 아직 DRAFT 상태인 근무표
        createCandidate(
            draftSchedule,
            requesterMember,
            candidateMember,
            now.plusDays(8),
            ShiftStatus.SCHEDULED,
            RequestStatus.OPEN,
            CandidateStatus.PENDING
        );

        // 6. 취소된 Shift
        createCandidate(
            publishedSchedule,
            requesterMember,
            candidateMember,
            now.plusHours(6),
            ShiftStatus.CANCELLED,
            RequestStatus.OPEN,
            CandidateStatus.PENDING
        );

        entityManager.flush();
        entityManager.clear();

        // when
        List<SubstituteCandidate> result =
            substituteCandidateRepository.findReceivedRequests(
                candidateUser.getId(), // 이 사용자에게 온 요청 조회
                now                    // 현재 시간 기준
            );

        // then
        assertThat(result)
            .extracting(SubstituteCandidate::getId)
            .containsExactly(validCandidate.getId());
    }

    @Test
    @DisplayName("SUB-07 - 내가 수락한 승인 대기 요청만 조회한다")
    void findAcceptedPendingRequests_Success() {

        // given
        LocalDateTime now = LocalDateTime.of(2026, 9, 23, 14, 0);

        Workplace workplace = entityManager.persist(
            new Workplace("SWITCH 카페", "TEST5678")
        );

        User requesterUser = entityManager.persist(
            new User("requester2@test.com", "password", "요청자")
        );

        User candidateUser = entityManager.persist(
            new User("candidate2@test.com", "password", "후보자")
        );

        WorkplaceMember requesterMember = entityManager.persist(
            new WorkplaceMember(
                workplace,
                requesterUser,
                WorkplaceRole.EMPLOYEE,
                now.minusMonths(1)
            )
        );

        WorkplaceMember candidateMember = entityManager.persist(
            new WorkplaceMember(
                workplace,
                candidateUser,
                WorkplaceRole.EMPLOYEE,
                now.minusMonths(1)
            )
        );

        // 공개된 근무표
        Schedule publishedSchedule =
            new Schedule(workplace, LocalDate.of(2026, 9, 21));

        publishedSchedule.publish(now.minusDays(1));
        entityManager.persist(publishedSchedule);

        // 아직 공개되지 않은 근무표
        Schedule draftSchedule =
            new Schedule(workplace, LocalDate.of(2026, 9, 28));

        entityManager.persist(draftSchedule);

        // 1. 모든 조건을 만족 → 조회되어야 함
        SubstituteCandidate validCandidate = createCandidate(
            publishedSchedule,
            requesterMember,
            candidateMember,
            now.plusHours(2),
            ShiftStatus.SCHEDULED,
            RequestStatus.ACCEPTED,
            CandidateStatus.ACCEPTED
        );

        // 2. 아직 수락하지 않은 Candidate → 제외
        createCandidate(
            publishedSchedule,
            requesterMember,
            candidateMember,
            now.plusHours(3),
            ShiftStatus.SCHEDULED,
            RequestStatus.ACCEPTED,
            CandidateStatus.PENDING
        );

        // 3. 이미 최종 승인된 Request → 제외
        createCandidate(
            publishedSchedule,
            requesterMember,
            candidateMember,
            now.plusHours(4),
            ShiftStatus.SCHEDULED,
            RequestStatus.APPROVED,
            CandidateStatus.ACCEPTED
        );

        // 4. 종료된 Request → 제외
        createCandidate(
            publishedSchedule,
            requesterMember,
            candidateMember,
            now.plusHours(5),
            ShiftStatus.SCHEDULED,
            RequestStatus.CLOSED,
            CandidateStatus.ACCEPTED
        );

        // 5. 이미 시작된 Shift → 제외
        createCandidate(
            publishedSchedule,
            requesterMember,
            candidateMember,
            now.minusHours(1),
            ShiftStatus.SCHEDULED,
            RequestStatus.ACCEPTED,
            CandidateStatus.ACCEPTED
        );

        // 6. DRAFT 근무표 → 제외
        createCandidate(
            draftSchedule,
            requesterMember,
            candidateMember,
            now.plusDays(8),
            ShiftStatus.SCHEDULED,
            RequestStatus.ACCEPTED,
            CandidateStatus.ACCEPTED
        );

        // 7. 취소된 Shift → 제외
        createCandidate(
            publishedSchedule,
            requesterMember,
            candidateMember,
            now.plusHours(7),
            ShiftStatus.CANCELLED,
            RequestStatus.ACCEPTED,
            CandidateStatus.ACCEPTED
        );

        entityManager.flush();
        entityManager.clear();

        // when
        List<SubstituteCandidate> result =
            substituteCandidateRepository.findAcceptedPendingRequests(
                candidateUser.getId(), // 현재 로그인 User
                now                    // 현재 시간
            );

        // then
        assertThat(result)
            .extracting(SubstituteCandidate::getId)
            .containsExactly(validCandidate.getId());
    }


    private SubstituteCandidate createCandidate(
        Schedule schedule,                  // 어느 근무표의 Shift인지
        WorkplaceMember requesterMember,    // 대타 요청자
        WorkplaceMember candidateMember,    // 대타 후보자
        LocalDateTime startAt,              // 근무 시작 시간
        ShiftStatus shiftStatus,            // SCHEDULED / CANCELLED
        RequestStatus requestStatus,        // OPEN / CLOSED 등
        CandidateStatus candidateStatus     // PENDING / REJECTED 등
    ) {

        Shift shift = entityManager.persist(
            new Shift(
                schedule,
                requesterMember,
                startAt,
                startAt.plusHours(4),
                shiftStatus
            )
        );

        SubstituteRequest request = new SubstituteRequest();

        // SubstituteRequest에 아직 생성자가 없어서
        // 테스트에서만 값을 넣어주는 코드
        ReflectionTestUtils.setField(request, "shift", shift);
        ReflectionTestUtils.setField(
            request,
            "requesterMember",
            requesterMember
        );
        ReflectionTestUtils.setField(
            request,
            "status",
            requestStatus
        );

        entityManager.persist(request);

        SubstituteCandidate candidate =
            new SubstituteCandidate(request, candidateMember);

        // 생성자는 기본적으로 PENDING이므로,
        // REJECTED 같은 다른 상태 테스트가 필요할 때만 바꿔준다.
        ReflectionTestUtils.setField(
            candidate,
            "status",
            candidateStatus
        );

        return entityManager.persist(candidate);
    }
}