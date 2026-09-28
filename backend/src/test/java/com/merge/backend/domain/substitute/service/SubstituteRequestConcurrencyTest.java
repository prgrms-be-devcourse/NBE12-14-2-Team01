package com.merge.backend.domain.substitute.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.merge.backend.domain.shift.entity.Schedule;
import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.entity.ShiftStatus;
import com.merge.backend.domain.shift.repository.ScheduleRepository;
import com.merge.backend.domain.shift.repository.ShiftRepository;
import com.merge.backend.domain.substitute.entity.RequestStatus;
import com.merge.backend.domain.substitute.entity.SubstituteRequest;
import com.merge.backend.domain.substitute.exception.SubstituteRequestErrorCode;
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

    private record Fixture(
        Long requesterUserId,
        Long shiftId
    ) {
    }

}