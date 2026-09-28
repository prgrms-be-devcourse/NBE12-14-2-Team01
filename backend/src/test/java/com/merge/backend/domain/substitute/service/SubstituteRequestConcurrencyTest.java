package com.merge.backend.domain.substitute.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.merge.backend.domain.shift.entity.Schedule;
import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.entity.ShiftStatus;
import com.merge.backend.domain.shift.repository.ScheduleRepository;
import com.merge.backend.domain.shift.repository.ShiftRepository;
import com.merge.backend.domain.substitute.entity.CandidateStatus;
import com.merge.backend.domain.substitute.entity.RequestStatus;
import com.merge.backend.domain.substitute.entity.SubstituteCandidate;
import com.merge.backend.domain.substitute.entity.SubstituteRequest;
import com.merge.backend.domain.substitute.exception.SubstituteRequestErrorCode;
import com.merge.backend.domain.substitute.repository.SubstituteCandidateRepository;
import com.merge.backend.domain.substitute.repository.SubstituteRequestRepository;
import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.user.repository.UserRepository;
import com.merge.backend.domain.workplace.entity.Workplace;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;
import com.merge.backend.domain.workplace.repository.WorkplaceMemberRepository;
import com.merge.backend.domain.workplace.repository.WorkplaceRepository;
import com.merge.backend.global.exception.BusinessException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class SubstituteRequestConcurrencyTest {

    @Autowired
    private SubstituteRequestService substituteRequestService;

    @Autowired
    private SubstituteRequestRepository substituteRequestRepository;

    @Autowired
    private SubstituteCandidateService substituteCandidateService;

    @Autowired
    private SubstituteCandidateRepository substituteCandidateRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkplaceRepository workplaceRepository;

    @Autowired
    private WorkplaceMemberRepository workplaceMemberRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private Clock clock;

    @Test
    @DisplayName(
        "같은 Shift에 대타 요청을 동시에 생성해도 활성 요청은 하나만 생성된다"
    )
    void concurrentCreateAllowsOnlyOneActiveRequest()
        throws Exception {

        // given
        Fixture fixture =
            createFixture("create-create");

        ExecutorService executor =
            Executors.newFixedThreadPool(2);

        CountDownLatch ready =
            new CountDownLatch(2);

        CountDownLatch start =
            new CountDownLatch(1);

        try {

            Future<Throwable> firstFuture =
                executor.submit(() -> {

                    ready.countDown();
                    start.await();

                    try {
                        substituteRequestService.create(
                            fixture.shiftId(),
                            fixture.requesterUserId()
                        );

                        return null;

                    } catch (Throwable throwable) {
                        return throwable;
                    }
                });

            Future<Throwable> secondFuture =
                executor.submit(() -> {

                    ready.countDown();
                    start.await();

                    try {
                        substituteRequestService.create(
                            fixture.shiftId(),
                            fixture.requesterUserId()
                        );

                        return null;

                    } catch (Throwable throwable) {
                        return throwable;
                    }
                });

            assertThat(
                ready.await(
                    3,
                    TimeUnit.SECONDS
                )
            ).isTrue();

            start.countDown();

            Throwable firstError =
                firstFuture.get(
                    5,
                    TimeUnit.SECONDS
                );

            Throwable secondError =
                secondFuture.get(
                    5,
                    TimeUnit.SECONDS
                );

            boolean firstSucceeded =
                firstError == null;

            boolean secondSucceeded =
                secondError == null;

            assertThat(
                firstSucceeded ^ secondSucceeded
            ).isTrue();

            Throwable failedError =
                firstError != null
                    ? firstError
                    : secondError;

            assertThat(failedError)
                .isInstanceOf(BusinessException.class);

            BusinessException businessException =
                (BusinessException) failedError;

            assertThat(
                businessException.getErrorCode()
            ).isEqualTo(
                SubstituteRequestErrorCode.ACTIVE_REQUEST_EXISTS
            );

            TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

            long activeRequestCount =
                transactionTemplate.execute(status -> {

                    List<SubstituteRequest> requests =
                        substituteRequestRepository
                            .findAllByRequesterMember_User_IdOrderByCreateDateDesc(
                                fixture.requesterUserId()
                            );

                    return requests.stream()
                        .filter(request ->
                            request.getShift()
                                .getId()
                                .equals(fixture.shiftId())
                        )
                        .filter(request ->
                            request.getStatus()
                                == RequestStatus.OPEN
                                || request.getStatus()
                                == RequestStatus.ACCEPTED
                        )
                        .count();
                });

            assertThat(activeRequestCount)
                .isEqualTo(1);

        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName(
        "같은 Request의 두 Candidate가 동시에 수락해도 한 명만 ACCEPTED 된다"
    )
    void concurrentAcceptAllowsOnlyOneCandidate()
        throws Exception {

        // given
        TransitionFixture fixture =
            createTransitionFixture("accept-accept");

        ExecutorService executor =
            Executors.newFixedThreadPool(2);

        CountDownLatch ready =
            new CountDownLatch(2);

        CountDownLatch start =
            new CountDownLatch(1);

        try {

            Future<Throwable> firstFuture =
                executor.submit(() -> {

                    ready.countDown();
                    start.await();

                    try {
                        substituteCandidateService.acceptCandidate(
                            fixture.firstCandidateId(),
                            fixture.firstCandidateUserId()
                        );

                        return null;

                    } catch (Throwable throwable) {
                        return throwable;
                    }
                });

            Future<Throwable> secondFuture =
                executor.submit(() -> {

                    ready.countDown();
                    start.await();

                    try {
                        substituteCandidateService.acceptCandidate(
                            fixture.secondCandidateId(),
                            fixture.secondCandidateUserId()
                        );

                        return null;

                    } catch (Throwable throwable) {
                        return throwable;
                    }
                });

            assertThat(
                ready.await(
                    3,
                    TimeUnit.SECONDS
                )
            ).isTrue();

            start.countDown();

            Throwable firstError =
                firstFuture.get(
                    5,
                    TimeUnit.SECONDS
                );

            Throwable secondError =
                secondFuture.get(
                    5,
                    TimeUnit.SECONDS
                );

            boolean firstSucceeded =
                firstError == null;

            boolean secondSucceeded =
                secondError == null;

            assertThat(
                firstSucceeded ^ secondSucceeded
            ).isTrue();

            Throwable failedError =
                firstError != null
                    ? firstError
                    : secondError;

            assertThat(failedError)
                .isInstanceOf(BusinessException.class);

            BusinessException businessException =
                (BusinessException) failedError;

            assertThat(
                businessException.getErrorCode()
            ).isEqualTo(
                SubstituteRequestErrorCode.REQUEST_NOT_OPEN
            );

            TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

            transactionTemplate.executeWithoutResult(status -> {

                SubstituteRequest request =
                    substituteRequestRepository
                        .findById(fixture.requestId())
                        .orElseThrow();

                SubstituteCandidate firstCandidate =
                    substituteCandidateRepository
                        .findById(fixture.firstCandidateId())
                        .orElseThrow();

                SubstituteCandidate secondCandidate =
                    substituteCandidateRepository
                        .findById(fixture.secondCandidateId())
                        .orElseThrow();

                assertThat(request.getStatus())
                    .isEqualTo(RequestStatus.ACCEPTED);

                assertThat(
                    List.of(
                        firstCandidate.getStatus(),
                        secondCandidate.getStatus()
                    )
                ).containsExactlyInAnyOrder(
                    CandidateStatus.ACCEPTED,
                    CandidateStatus.PENDING
                );
            });

        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName(
        "같은 Request의 마지막 두 Candidate가 동시에 거절해도 Request는 CLOSED 된다"
    )
    void concurrentRejectClosesRequestWhenNoPendingCandidateRemains()
        throws Exception {

        // given
        TransitionFixture fixture =
            createTransitionFixture("reject-reject");

        ExecutorService executor =
            Executors.newFixedThreadPool(2);

        CountDownLatch ready =
            new CountDownLatch(2);

        CountDownLatch start =
            new CountDownLatch(1);

        try {

            Future<Throwable> firstFuture =
                executor.submit(() -> {

                    ready.countDown();
                    start.await();

                    try {
                        substituteCandidateService.rejectCandidate(
                            fixture.firstCandidateId(),
                            fixture.firstCandidateUserId()
                        );

                        return null;

                    } catch (Throwable throwable) {
                        return throwable;
                    }
                });

            Future<Throwable> secondFuture =
                executor.submit(() -> {

                    ready.countDown();
                    start.await();

                    try {
                        substituteCandidateService.rejectCandidate(
                            fixture.secondCandidateId(),
                            fixture.secondCandidateUserId()
                        );

                        return null;

                    } catch (Throwable throwable) {
                        return throwable;
                    }
                });

            assertThat(
                ready.await(
                    3,
                    TimeUnit.SECONDS
                )
            ).isTrue();

            start.countDown();

            Throwable firstError =
                firstFuture.get(
                    5,
                    TimeUnit.SECONDS
                );

            Throwable secondError =
                secondFuture.get(
                    5,
                    TimeUnit.SECONDS
                );

            assertThat(firstError)
                .isNull();

            assertThat(secondError)
                .isNull();

            TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

            transactionTemplate.executeWithoutResult(status -> {

                SubstituteRequest request =
                    substituteRequestRepository
                        .findById(fixture.requestId())
                        .orElseThrow();

                SubstituteCandidate firstCandidate =
                    substituteCandidateRepository
                        .findById(fixture.firstCandidateId())
                        .orElseThrow();

                SubstituteCandidate secondCandidate =
                    substituteCandidateRepository
                        .findById(fixture.secondCandidateId())
                        .orElseThrow();

                assertThat(request.getStatus())
                    .isEqualTo(RequestStatus.CLOSED);

                assertThat(firstCandidate.getStatus())
                    .isEqualTo(CandidateStatus.REJECTED);

                assertThat(secondCandidate.getStatus())
                    .isEqualTo(CandidateStatus.REJECTED);
            });

        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName(
        "같은 User가 겹치는 서로 다른 Request를 동시에 수락해도 하나만 ACCEPTED 된다"
    )
    void concurrentAcceptOnDifferentRequestsAllowsOnlyOneForSameUser()
        throws Exception {

        // given
        UserConflictFixture fixture =
            createUserConflictFixture(
                "user-accept-accept"
            );

        ExecutorService executor =
            Executors.newFixedThreadPool(2);

        CountDownLatch ready =
            new CountDownLatch(2);

        CountDownLatch start =
            new CountDownLatch(1);

        try {

            Future<Throwable> firstFuture =
                executor.submit(() -> {

                    ready.countDown();
                    start.await();

                    try {
                        substituteCandidateService.acceptCandidate(
                            fixture.firstCandidateId(),
                            fixture.candidateUserId()
                        );

                        return null;

                    } catch (Throwable throwable) {
                        return throwable;
                    }
                });

            Future<Throwable> secondFuture =
                executor.submit(() -> {

                    ready.countDown();
                    start.await();

                    try {
                        substituteCandidateService.acceptCandidate(
                            fixture.secondCandidateId(),
                            fixture.candidateUserId()
                        );

                        return null;

                    } catch (Throwable throwable) {
                        return throwable;
                    }
                });

            assertThat(
                ready.await(
                    3,
                    TimeUnit.SECONDS
                )
            ).isTrue();

            start.countDown();

            Throwable firstError =
                firstFuture.get(
                    5,
                    TimeUnit.SECONDS
                );

            Throwable secondError =
                secondFuture.get(
                    5,
                    TimeUnit.SECONDS
                );

            boolean firstSucceeded =
                firstError == null;

            boolean secondSucceeded =
                secondError == null;

            // 정확히 한 요청만 성공해야 함
            assertThat(
                firstSucceeded ^ secondSucceeded
            ).isTrue();

            Throwable failedError =
                firstError != null
                    ? firstError
                    : secondError;

            assertThat(failedError)
                .isInstanceOf(BusinessException.class);

            BusinessException businessException =
                (BusinessException) failedError;

            assertThat(
                businessException.getErrorCode()
            ).isEqualTo(
                SubstituteRequestErrorCode.CONFLICT_ACTIVE_SUBSTITUTE
            );

            // 최종 DB 상태는 새로운 Transaction에서 다시 확인
            TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

            transactionTemplate.executeWithoutResult(status -> {

                SubstituteRequest firstRequest =
                    substituteRequestRepository
                        .findById(fixture.firstRequestId())
                        .orElseThrow();

                SubstituteRequest secondRequest =
                    substituteRequestRepository
                        .findById(fixture.secondRequestId())
                        .orElseThrow();

                SubstituteCandidate firstCandidate =
                    substituteCandidateRepository
                        .findById(fixture.firstCandidateId())
                        .orElseThrow();

                SubstituteCandidate secondCandidate =
                    substituteCandidateRepository
                        .findById(fixture.secondCandidateId())
                        .orElseThrow();

                assertThat(
                    List.of(
                        firstRequest.getStatus(),
                        secondRequest.getStatus()
                    )
                ).containsExactlyInAnyOrder(
                    RequestStatus.ACCEPTED,
                    RequestStatus.OPEN
                );

                assertThat(
                    List.of(
                        firstCandidate.getStatus(),
                        secondCandidate.getStatus()
                    )
                ).containsExactlyInAnyOrder(
                    CandidateStatus.ACCEPTED,
                    CandidateStatus.PENDING
                );
            });

        } finally {
            executor.shutdownNow();
        }
    }

    private Fixture createFixture(String suffix) {

        TransactionTemplate transactionTemplate =
            new TransactionTemplate(transactionManager);

        return transactionTemplate.execute(status -> {

            LocalDateTime now =
                LocalDateTime.now(clock);

            LocalDateTime startAt =
                now.plusDays(7)
                    .withHour(10)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);

            LocalDateTime endAt =
                startAt.plusHours(4);

            LocalDate weekStartDate =
                startAt.toLocalDate()
                    .with(
                        TemporalAdjusters.previousOrSame(
                            DayOfWeek.MONDAY
                        )
                    );

            User requesterUser =
                userRepository.save(
                    new User(
                        "requester-" + suffix + "@test.com",
                        "password-hash",
                        "requester"
                    )
                );

            User candidateUser =
                userRepository.save(
                    new User(
                        "candidate-" + suffix + "@test.com",
                        "password-hash",
                        "candidate"
                    )
                );

            Workplace workplace =
                workplaceRepository.save(
                    new Workplace(
                        "대타 요청 동시성 테스트 매장 " + suffix,
                        "SUB-" + suffix
                    )
                );

            LocalDateTime joinedAt =
                now.minusDays(30);

            WorkplaceMember requesterMember =
                workplaceMemberRepository.save(
                    new WorkplaceMember(
                        workplace,
                        requesterUser,
                        WorkplaceRole.EMPLOYEE,
                        joinedAt
                    )
                );

            workplaceMemberRepository.save(
                new WorkplaceMember(
                    workplace,
                    candidateUser,
                    WorkplaceRole.EMPLOYEE,
                    joinedAt
                )
            );

            Schedule schedule =
                new Schedule(
                    workplace,
                    weekStartDate
                );

            schedule.publish(now);

            schedule =
                scheduleRepository.save(schedule);

            Shift shift =
                shiftRepository.save(
                    new Shift(
                        schedule,
                        requesterMember,
                        startAt,
                        endAt,
                        ShiftStatus.SCHEDULED
                    )
                );

            return new Fixture(
                requesterUser.getId(),
                shift.getId()
            );
        });
    }

    private TransitionFixture createTransitionFixture(String suffix) {

        TransactionTemplate transactionTemplate =
            new TransactionTemplate(transactionManager);

        return transactionTemplate.execute(status -> {

            LocalDateTime now =
                LocalDateTime.now(clock);

            LocalDateTime startAt =
                now.plusDays(7)
                    .withHour(10)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);

            LocalDateTime endAt =
                startAt.plusHours(4);

            LocalDate weekStartDate =
                startAt.toLocalDate()
                    .with(
                        TemporalAdjusters.previousOrSame(
                            DayOfWeek.MONDAY
                        )
                    );

            User requesterUser =
                userRepository.save(
                    new User(
                        "transition-requester-" + suffix + "@test.com",
                        "password-hash",
                        "requester"
                    )
                );

            User firstCandidateUser =
                userRepository.save(
                    new User(
                        "transition-first-" + suffix + "@test.com",
                        "password-hash",
                        "first-candidate"
                    )
                );

            User secondCandidateUser =
                userRepository.save(
                    new User(
                        "transition-second-" + suffix + "@test.com",
                        "password-hash",
                        "second-candidate"
                    )
                );

            Workplace workplace =
                workplaceRepository.save(
                    new Workplace(
                        "대타 상태 전이 동시성 테스트 매장 " + suffix,
                        "TR-" + suffix
                    )
                );

            LocalDateTime joinedAt =
                now.minusDays(30);

            WorkplaceMember requesterMember =
                workplaceMemberRepository.save(
                    new WorkplaceMember(
                        workplace,
                        requesterUser,
                        WorkplaceRole.EMPLOYEE,
                        joinedAt
                    )
                );

            WorkplaceMember firstCandidateMember =
                workplaceMemberRepository.save(
                    new WorkplaceMember(
                        workplace,
                        firstCandidateUser,
                        WorkplaceRole.EMPLOYEE,
                        joinedAt
                    )
                );

            WorkplaceMember secondCandidateMember =
                workplaceMemberRepository.save(
                    new WorkplaceMember(
                        workplace,
                        secondCandidateUser,
                        WorkplaceRole.EMPLOYEE,
                        joinedAt
                    )
                );

            Schedule schedule =
                new Schedule(
                    workplace,
                    weekStartDate
                );

            schedule.publish(now);

            schedule =
                scheduleRepository.save(schedule);

            Shift shift =
                shiftRepository.save(
                    new Shift(
                        schedule,
                        requesterMember,
                        startAt,
                        endAt,
                        ShiftStatus.SCHEDULED
                    )
                );

            SubstituteRequest request =
                substituteRequestRepository.save(
                    new SubstituteRequest(
                        shift,
                        requesterMember,
                        RequestStatus.OPEN
                    )
                );

            SubstituteCandidate firstCandidate =
                substituteCandidateRepository.save(
                    new SubstituteCandidate(
                        request,
                        firstCandidateMember
                    )
                );

            SubstituteCandidate secondCandidate =
                substituteCandidateRepository.save(
                    new SubstituteCandidate(
                        request,
                        secondCandidateMember
                    )
                );

            return new TransitionFixture(
                request.getId(),
                firstCandidate.getId(),
                firstCandidateUser.getId(),
                secondCandidate.getId(),
                secondCandidateUser.getId()
            );
        });
    }

    private UserConflictFixture createUserConflictFixture(
        String suffix
    ) {
        TransactionTemplate transactionTemplate =
            new TransactionTemplate(transactionManager);

        return transactionTemplate.execute(status -> {

            LocalDateTime now =
                LocalDateTime.now(clock);

            LocalDateTime firstStartAt =
                now.plusDays(7)
                    .withHour(10)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);

            LocalDateTime firstEndAt =
                firstStartAt.plusHours(4);

            LocalDateTime secondStartAt =
                firstStartAt.plusHours(2);

            LocalDateTime secondEndAt =
                secondStartAt.plusHours(4);

            LocalDate weekStartDate =
                firstStartAt.toLocalDate()
                    .with(
                        TemporalAdjusters.previousOrSame(
                            DayOfWeek.MONDAY
                        )
                    );

            User firstRequesterUser =
                userRepository.save(
                    new User(
                        "user-conflict-requester-first-"
                            + suffix
                            + "@test.com",
                        "password-hash",
                        "first-requester"
                    )
                );

            User secondRequesterUser =
                userRepository.save(
                    new User(
                        "user-conflict-requester-second-"
                            + suffix
                            + "@test.com",
                        "password-hash",
                        "second-requester"
                    )
                );

            User candidateUser =
                userRepository.save(
                    new User(
                        "user-conflict-candidate-"
                            + suffix
                            + "@test.com",
                        "password-hash",
                        "candidate"
                    )
                );

            Workplace workplace =
                workplaceRepository.save(
                    new Workplace(
                        "대타 User 충돌 테스트 매장 " + suffix,
                        "UC-" + suffix
                    )
                );

            LocalDateTime joinedAt =
                now.minusDays(30);

            WorkplaceMember firstRequesterMember =
                workplaceMemberRepository.save(
                    new WorkplaceMember(
                        workplace,
                        firstRequesterUser,
                        WorkplaceRole.EMPLOYEE,
                        joinedAt
                    )
                );

            WorkplaceMember secondRequesterMember =
                workplaceMemberRepository.save(
                    new WorkplaceMember(
                        workplace,
                        secondRequesterUser,
                        WorkplaceRole.EMPLOYEE,
                        joinedAt
                    )
                );

            WorkplaceMember candidateMember =
                workplaceMemberRepository.save(
                    new WorkplaceMember(
                        workplace,
                        candidateUser,
                        WorkplaceRole.EMPLOYEE,
                        joinedAt
                    )
                );

            Schedule schedule =
                new Schedule(
                    workplace,
                    weekStartDate
                );

            schedule.publish(now);

            schedule =
                scheduleRepository.save(schedule);

            Shift firstShift =
                shiftRepository.save(
                    new Shift(
                        schedule,
                        firstRequesterMember,
                        firstStartAt,
                        firstEndAt,
                        ShiftStatus.SCHEDULED
                    )
                );

            Shift secondShift =
                shiftRepository.save(
                    new Shift(
                        schedule,
                        secondRequesterMember,
                        secondStartAt,
                        secondEndAt,
                        ShiftStatus.SCHEDULED
                    )
                );

            SubstituteRequest firstRequest =
                substituteRequestRepository.save(
                    new SubstituteRequest(
                        firstShift,
                        firstRequesterMember,
                        RequestStatus.OPEN
                    )
                );

            SubstituteRequest secondRequest =
                substituteRequestRepository.save(
                    new SubstituteRequest(
                        secondShift,
                        secondRequesterMember,
                        RequestStatus.OPEN
                    )
                );

            SubstituteCandidate firstCandidate =
                substituteCandidateRepository.save(
                    new SubstituteCandidate(
                        firstRequest,
                        candidateMember
                    )
                );

            SubstituteCandidate secondCandidate =
                substituteCandidateRepository.save(
                    new SubstituteCandidate(
                        secondRequest,
                        candidateMember
                    )
                );

            return new UserConflictFixture(
                firstRequest.getId(),
                secondRequest.getId(),
                firstCandidate.getId(),
                secondCandidate.getId(),
                candidateUser.getId()
            );
        });
    }

    private record Fixture(
        Long requesterUserId,
        Long shiftId
    ) {
    }

    private record TransitionFixture(
        Long requestId,
        Long firstCandidateId,
        Long firstCandidateUserId,
        Long secondCandidateId,
        Long secondCandidateUserId
    ) {
    }

    private record UserConflictFixture(
        Long firstRequestId,
        Long secondRequestId,
        Long firstCandidateId,
        Long secondCandidateId,
        Long candidateUserId
    ) {
    }
}