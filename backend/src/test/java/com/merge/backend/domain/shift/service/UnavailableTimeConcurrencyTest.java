package com.merge.backend.domain.shift.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.merge.backend.domain.shift.entity.Schedule;
import com.merge.backend.domain.shift.entity.ScheduleStatus;
import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.entity.ShiftStatus;
import com.merge.backend.domain.shift.entity.UnavailableTime;
import com.merge.backend.domain.shift.exception.ShiftErrorCode;
import com.merge.backend.domain.shift.exception.UnavailableTimeErrorCode;
import com.merge.backend.domain.shift.repository.ScheduleRepository;
import com.merge.backend.domain.shift.repository.ShiftRepository;
import com.merge.backend.domain.shift.repository.UnavailableTimeRepository;
import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.user.repository.UserRepository;
import com.merge.backend.domain.workplace.entity.Workplace;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;
import com.merge.backend.domain.workplace.repository.WorkplaceMemberRepository;
import com.merge.backend.domain.workplace.repository.WorkplaceRepository;
import com.merge.backend.global.exception.BusinessException;
import com.merge.backend.global.rq.Rq;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class UnavailableTimeConcurrencyTest {

    @Autowired
    private UnavailableTimeService unavailableTimeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private WorkplaceRepository workplaceRepository;

    @Autowired
    private WorkplaceMemberRepository workplaceMemberRepository;

    @Autowired
    private UnavailableTimeRepository unavailableTimeRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private Rq rq;

    @Test
    @DisplayName(
        "같은 User의 겹치는 UnavailableTime을 동시에 등록해도 하나만 생성된다"
    )
    void concurrentRegisterAllowsOnlyOneOverlappingUnavailableTime()
        throws Exception {

        // given
        Long userId =
            createUser("register-register");

        when(rq.getActorId())
            .thenReturn(userId);

        LocalDateTime firstStartAt =
            LocalDateTime.of(2026, 10, 10, 10, 0);

        LocalDateTime firstEndAt =
            LocalDateTime.of(2026, 10, 10, 14, 0);

        LocalDateTime secondStartAt =
            LocalDateTime.of(2026, 10, 10, 12, 0);

        LocalDateTime secondEndAt =
            LocalDateTime.of(2026, 10, 10, 16, 0);

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
                        unavailableTimeService.register(
                            firstStartAt,
                            firstEndAt,
                            false
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
                        unavailableTimeService.register(
                            secondStartAt,
                            secondEndAt,
                            false
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

            // 둘 중 정확히 하나만 성공
            boolean firstSucceeded =
                firstError == null;

            boolean secondSucceeded =
                secondError == null;

            assertThat(
                firstSucceeded ^ secondSucceeded
            ).isTrue();

            // 실패한 하나는 overlap 비즈니스 예외
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
                UnavailableTimeErrorCode.TIME_OVERLAP
            );

            // 최종 DB에도 하나만 존재
            List<UnavailableTime> finalUnavailableTimes =
                unavailableTimeRepository
                    .findByUserId(userId);

            assertThat(finalUnavailableTimes)
                .hasSize(1);

            UnavailableTime finalUnavailableTime =
                finalUnavailableTimes.get(0);

            boolean firstPersisted =
                finalUnavailableTime.getStartAt()
                    .equals(firstStartAt)
                    && finalUnavailableTime.getEndAt()
                    .equals(firstEndAt);

            boolean secondPersisted =
                finalUnavailableTime.getStartAt()
                    .equals(secondStartAt)
                    && finalUnavailableTime.getEndAt()
                    .equals(secondEndAt);

            assertThat(
                firstPersisted || secondPersisted
            ).isTrue();

        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("같은 User의 등록과 수정이 동시에 겹치는 시간을 만들려 해도 하나만 성공한다")
    void concurrentRegisterAndUpdateDoNotCreateOverlap()
        throws Exception {

        // given
        Long userId =
            createUser("register-update");

        when(rq.getActorId())
            .thenReturn(userId);

        LocalDateTime originalStartAt =
            LocalDateTime.of(
                2026, 10, 11, 9, 0
            );

        LocalDateTime originalEndAt =
            LocalDateTime.of(
                2026, 10, 11, 10, 0
            );

        Long unavailableTimeId =
            createUnavailableTime(
                userId,
                originalStartAt,
                originalEndAt
            );

        /*
         * 새 등록
         * 12:00 ~ 14:00
         */
        LocalDateTime registerStartAt =
            LocalDateTime.of(
                2026, 10, 11, 12, 0
            );

        LocalDateTime registerEndAt =
            LocalDateTime.of(
                2026, 10, 11, 14, 0
            );

        /*
         * 기존 일정 수정
         * 09:00 ~ 10:00
         *        ↓
         * 13:00 ~ 15:00
         *
         * 새 등록 시간과 13:00 ~ 14:00이 겹친다.
         */
        LocalDateTime updateStartAt =
            LocalDateTime.of(
                2026, 10, 11, 13, 0
            );

        LocalDateTime updateEndAt =
            LocalDateTime.of(
                2026, 10, 11, 15, 0
            );

        ExecutorService executor =
            Executors.newFixedThreadPool(2);

        CountDownLatch ready =
            new CountDownLatch(2);

        CountDownLatch start =
            new CountDownLatch(1);

        try {

            Future<Throwable> registerFuture =
                executor.submit(() -> {

                    ready.countDown();
                    start.await();

                    try {
                        unavailableTimeService.register(
                            registerStartAt,
                            registerEndAt,
                            false
                        );

                        return null;

                    } catch (Throwable throwable) {
                        return throwable;
                    }
                });

            Future<Throwable> updateFuture =
                executor.submit(() -> {

                    ready.countDown();
                    start.await();

                    try {
                        unavailableTimeService.update(
                            unavailableTimeId,
                            updateStartAt,
                            updateEndAt,
                            false
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

            Throwable registerError =
                registerFuture.get(
                    5,
                    TimeUnit.SECONDS
                );

            Throwable updateError =
                updateFuture.get(
                    5,
                    TimeUnit.SECONDS
                );

            boolean registerSucceeded =
                registerError == null;

            boolean updateSucceeded =
                updateError == null;

            // 둘 중 정확히 하나만 성공
            assertThat(
                registerSucceeded ^ updateSucceeded
            ).isTrue();

            Throwable failedError =
                registerError != null
                    ? registerError
                    : updateError;

            assertThat(failedError)
                .isInstanceOf(
                    BusinessException.class
                );

            BusinessException businessException =
                (BusinessException) failedError;

            assertThat(
                businessException.getErrorCode()
            ).isEqualTo(
                UnavailableTimeErrorCode.TIME_OVERLAP
            );

            // 최종 DB 상태 확인
            List<UnavailableTime> finalUnavailableTimes =
                unavailableTimeRepository
                    .findByUserId(userId);

            boolean registerWon =
                finalUnavailableTimes.size() == 2
                    && finalUnavailableTimes.stream()
                    .anyMatch(unavailableTime ->
                        unavailableTime.getId()
                            .equals(unavailableTimeId)
                            && unavailableTime.getStartAt()
                            .equals(originalStartAt)
                            && unavailableTime.getEndAt()
                            .equals(originalEndAt)
                    )
                    && finalUnavailableTimes.stream()
                    .anyMatch(unavailableTime ->
                        unavailableTime.getStartAt()
                            .equals(registerStartAt)
                            && unavailableTime.getEndAt()
                            .equals(registerEndAt)
                    );

            boolean updateWon =
                finalUnavailableTimes.size() == 1
                    && finalUnavailableTimes.get(0)
                    .getId()
                    .equals(unavailableTimeId)
                    && finalUnavailableTimes.get(0)
                    .getStartAt()
                    .equals(updateStartAt)
                    && finalUnavailableTimes.get(0)
                    .getEndAt()
                    .equals(updateEndAt);

            assertThat(
                registerWon || updateWon
            ).isTrue();

        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName(
        "Schedule 공개와 UnavailableTime 등록이 동시에 실행되어도 확인 없는 충돌 상태는 만들어지지 않는다"
    )
    void concurrentPublishAndRegisterDoNotCreateUnconfirmedConflict()
        throws Exception {

        // given
        PublishFixture fixture =
            createPublishFixture(
                "publish-register"
            );

        when(rq.getActorId())
            .thenReturn(
                fixture.employeeUserId()
            );

        LocalDateTime unavailableStartAt =
            LocalDateTime.of(
                2026,
                10,
                6,
                10,
                0
            );

        LocalDateTime unavailableEndAt =
            LocalDateTime.of(
                2026,
                10,
                6,
                14,
                0
            );

        ExecutorService executor =
            Executors.newFixedThreadPool(2);

        CountDownLatch ready =
            new CountDownLatch(2);

        CountDownLatch start =
            new CountDownLatch(1);

        try {

            Future<Throwable> publishFuture =
                executor.submit(() -> {

                    ready.countDown();
                    start.await();

                    try {
                        scheduleService.publishSchedule(
                            fixture.managerUserId(),
                            fixture.workplaceId(),
                            fixture.scheduleId(),
                            false
                        );

                        return null;

                    } catch (Throwable throwable) {
                        return throwable;
                    }
                });

            Future<Throwable> registerFuture =
                executor.submit(() -> {

                    ready.countDown();
                    start.await();

                    try {
                        unavailableTimeService.register(
                            unavailableStartAt,
                            unavailableEndAt,
                            false
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

            Throwable publishError =
                publishFuture.get(
                    5,
                    TimeUnit.SECONDS
                );

            Throwable registerError =
                registerFuture.get(
                    5,
                    TimeUnit.SECONDS
                );

            boolean publishSucceeded =
                publishError == null;

            boolean registerSucceeded =
                registerError == null;

            /*
             * 둘 중 정확히 하나만 성공해야 한다.
             *
             * publish 먼저:
             *   publish 성공
             *   register → OFFICIAL_SHIFT_CONFLICT
             *
             * register 먼저:
             *   register 성공
             *   publish → UNAVAILABLE_TIME_CONFLICT
             */
            assertThat(
                publishSucceeded
                    ^ registerSucceeded
            ).isTrue();

            Schedule finalSchedule =
                scheduleRepository
                    .findById(
                        fixture.scheduleId()
                    )
                    .orElseThrow();

            List<UnavailableTime> finalUnavailableTimes =
                unavailableTimeRepository
                    .findByUserId(
                        fixture.employeeUserId()
                    );

            if (publishSucceeded) {

                assertThat(registerError)
                    .isInstanceOf(
                        BusinessException.class
                    );

                BusinessException businessException =
                    (BusinessException) registerError;

                assertThat(
                    businessException.getErrorCode()
                ).isEqualTo(
                    UnavailableTimeErrorCode
                        .OFFICIAL_SHIFT_CONFLICT
                );

                assertThat(
                    finalSchedule.getStatus()
                ).isEqualTo(
                    ScheduleStatus.PUBLISHED
                );

                assertThat(
                    finalUnavailableTimes
                ).isEmpty();

            } else {

                assertThat(publishError)
                    .isInstanceOf(
                        BusinessException.class
                    );

                BusinessException businessException =
                    (BusinessException) publishError;

                assertThat(
                    businessException.getErrorCode()
                ).isEqualTo(
                    ShiftErrorCode
                        .UNAVAILABLE_TIME_CONFLICT
                );

                assertThat(
                    finalSchedule.getStatus()
                ).isEqualTo(
                    ScheduleStatus.DRAFT
                );

                assertThat(
                    finalUnavailableTimes
                ).hasSize(1);

                UnavailableTime savedUnavailableTime =
                    finalUnavailableTimes.get(0);

                assertThat(
                    savedUnavailableTime.getStartAt()
                ).isEqualTo(
                    unavailableStartAt
                );

                assertThat(
                    savedUnavailableTime.getEndAt()
                ).isEqualTo(
                    unavailableEndAt
                );
            }

        } finally {
            executor.shutdownNow();
        }
    }

    private Long createUser(String suffix) {

        TransactionTemplate transactionTemplate =
            new TransactionTemplate(
                transactionManager
            );

        return transactionTemplate.execute(status -> {
            User user =
                userRepository.save(
                    new User(
                        "unavailable-concurrency-"
                            + suffix
                            + "@test.com",
                        "password-hash",
                        "test-user"
                    )
                );

            return user.getId();
        });
    }

    private Long createUnavailableTime(
        Long userId,
        LocalDateTime startAt,
        LocalDateTime endAt
    ) {

        TransactionTemplate transactionTemplate =
            new TransactionTemplate(
                transactionManager
            );

        return transactionTemplate.execute(status -> {

            User user =
                userRepository.findById(userId)
                    .orElseThrow();

            UnavailableTime unavailableTime =
                unavailableTimeRepository.save(
                    new UnavailableTime(
                        user,
                        startAt,
                        endAt
                    )
                );

            return unavailableTime.getId();
        });
    }

    private PublishFixture createPublishFixture(
        String suffix
    ) {

        TransactionTemplate transactionTemplate =
            new TransactionTemplate(
                transactionManager
            );

        return transactionTemplate.execute(status -> {

            User managerUser =
                userRepository.save(
                    new User(
                        "publish-manager-"
                            + suffix
                            + "@test.com",
                        "password-hash",
                        "manager"
                    )
                );

            User employeeUser =
                userRepository.save(
                    new User(
                        "publish-employee-"
                            + suffix
                            + "@test.com",
                        "password-hash",
                        "employee"
                    )
                );

            Workplace workplace =
                workplaceRepository.save(
                    new Workplace(
                        "UNA 공개 동시성 테스트 매장 "
                            + suffix,
                        "UNA-PUBLISH-" + suffix
                    )
                );

            LocalDateTime joinedAt =
                LocalDateTime.of(
                    2026,
                    9,
                    1,
                    9,
                    0
                );

            workplaceMemberRepository.save(
                new WorkplaceMember(
                    workplace,
                    managerUser,
                    WorkplaceRole.MANAGER,
                    joinedAt
                )
            );

            WorkplaceMember employeeMember =
                workplaceMemberRepository.save(
                    new WorkplaceMember(
                        workplace,
                        employeeUser,
                        WorkplaceRole.EMPLOYEE,
                        joinedAt
                    )
                );

            Schedule schedule =
                scheduleRepository.save(
                    new Schedule(
                        workplace,
                        LocalDate.of(
                            2026,
                            10,
                            5
                        )
                    )
                );

            shiftRepository.save(
                new Shift(
                    schedule,
                    employeeMember,
                    LocalDateTime.of(
                        2026,
                        10,
                        6,
                        10,
                        0
                    ),
                    LocalDateTime.of(
                        2026,
                        10,
                        6,
                        14,
                        0
                    ),
                    ShiftStatus.SCHEDULED
                )
            );

            return new PublishFixture(
                managerUser.getId(),
                employeeUser.getId(),
                workplace.getId(),
                schedule.getId()
            );
        });
    }

    private record PublishFixture(
        Long managerUserId,
        Long employeeUserId,
        Long workplaceId,
        Long scheduleId
    ) {

    }

}