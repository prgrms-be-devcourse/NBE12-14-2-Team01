package com.merge.backend.domain.shift.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.merge.backend.domain.notification.repository.NotificationRepository;
import com.merge.backend.domain.shift.dto.ShiftRequest;
import com.merge.backend.domain.shift.entity.Schedule;
import com.merge.backend.domain.shift.entity.ScheduleStatus;
import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.entity.ShiftStatus;
import com.merge.backend.domain.shift.exception.ScheduleErrorCode;
import com.merge.backend.domain.shift.exception.ShiftErrorCode;
import com.merge.backend.domain.shift.repository.ScheduleRepository;
import com.merge.backend.domain.shift.repository.ShiftRepository;
import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.user.repository.UserRepository;
import com.merge.backend.domain.workplace.entity.Workplace;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;
import com.merge.backend.domain.workplace.repository.WorkplaceMemberRepository;
import com.merge.backend.domain.workplace.repository.WorkplaceRepository;
import com.merge.backend.global.exception.BusinessException;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class ShiftScheduleConcurrencyTest {

    @Autowired
    private ShiftService shiftService;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkplaceRepository workplaceRepository;

    @Autowired
    private WorkplaceMemberRepository workplaceMemberRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName(
        "Schedule 공개와 마지막 Shift 삭제가 동시에 요청되어도 일관된 상태를 유지한다"
    )
    void publishAndDeleteAreSerializedByScheduleLock() throws Exception {

        // given
        Fixture fixture =
            createFixture("publish-delete");

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
                            true
                        );

                        return null;

                    } catch (Throwable throwable) {
                        return throwable;
                    }
                });

            Future<Throwable> deleteFuture =
                executor.submit(() -> {

                    ready.countDown();
                    start.await();

                    try {
                        shiftService.delete(
                            fixture.workplaceId(),
                            fixture.scheduleId(),
                            fixture.shiftId(),
                            fixture.managerUserId()
                        );

                        return null;

                    } catch (Throwable throwable) {
                        return throwable;
                    }
                });

            assertThat(
                ready.await(3, TimeUnit.SECONDS)
            ).isTrue();

            start.countDown();

            Throwable publishError =
                publishFuture.get(
                    5,
                    TimeUnit.SECONDS
                );

            Throwable deleteError =
                deleteFuture.get(
                    5,
                    TimeUnit.SECONDS
                );

            Schedule finalSchedule =
                scheduleRepository.findById(
                    fixture.scheduleId()
                ).orElseThrow();

            boolean finalShiftExists =
                shiftRepository.existsById(
                    fixture.shiftId()
                );

            boolean publishSucceeded =
                publishError == null;

            boolean deleteSucceeded =
                deleteError == null;

            assertThat(
                publishSucceeded ^ deleteSucceeded
            ).isTrue();

            if (publishSucceeded) {

                assertThat(deleteError)
                    .isInstanceOf(BusinessException.class);

                BusinessException businessException =
                    (BusinessException) deleteError;

                assertThat(businessException.getErrorCode())
                    .isEqualTo(
                        ShiftErrorCode.INVALID_STATUS_VALUE
                    );

                assertThat(finalSchedule.getStatus())
                    .isEqualTo(
                        ScheduleStatus.PUBLISHED
                    );

                assertThat(finalShiftExists)
                    .isTrue();

            } else {

                assertThat(publishError)
                    .isInstanceOf(BusinessException.class);

                BusinessException businessException =
                    (BusinessException) publishError;

                assertThat(businessException.getErrorCode())
                    .isEqualTo(
                        ScheduleErrorCode.EMPTY_SCHEDULE
                    );

                assertThat(finalSchedule.getStatus())
                    .isEqualTo(
                        ScheduleStatus.DRAFT
                    );

                assertThat(finalShiftExists)
                    .isFalse();
            }

        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName(
        "같은 Schedule에 겹치는 Shift가 동시에 추가되어도 하나만 생성된다"
    )
    void concurrentCreateAllowsOnlyOneOverlappingShift() throws Exception {

        // given
        Fixture fixture =
            createFixture("create-create");

        LocalDateTime startAt =
            LocalDateTime.of(
                2026, 9, 21,
                15, 0
            );

        LocalDateTime endAt =
            LocalDateTime.of(
                2026, 9, 21,
                19, 0
            );

        ShiftRequest request =
            new ShiftRequest(
                fixture.employeeMemberId(),
                startAt,
                endAt,
                true
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
                        shiftService.create(
                            request,
                            fixture.workplaceId(),
                            fixture.scheduleId(),
                            fixture.managerUserId()
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
                        shiftService.create(
                            request,
                            fixture.workplaceId(),
                            fixture.scheduleId(),
                            fixture.managerUserId()
                        );

                        return null;

                    } catch (Throwable throwable) {
                        return throwable;
                    }
                });

            assertThat(
                ready.await(3, TimeUnit.SECONDS)
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
                ShiftErrorCode.DUPLICATE_LOCAL_SCHEDULE_TIME
            );

            List<Shift> finalShifts =
                shiftRepository
                    .findBySchedule_IdOrderByStartAtAscIdAsc(
                        fixture.scheduleId()
                    );

            long createdShiftCount =
                finalShifts.stream()
                    .filter(shift ->
                        shift.getStartAt().equals(startAt)
                            && shift.getEndAt().equals(endAt)
                    )
                    .count();

            assertThat(createdShiftCount)
                .isEqualTo(1);

        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName(
        "Schedule 공개와 Shift 담당자 수정이 동시에 요청되어도 최종 담당자와 알림 수신자가 일치한다"
    )
    void publishAndModifyKeepAssigneeAndNotificationConsistent()
        throws Exception {

        // given
        Fixture fixture =
            createFixture("publish-modify");

        ShiftRequest modifyRequest =
            new ShiftRequest(
                fixture.replacementMemberId(),
                LocalDateTime.of(
                    2026, 9, 21,
                    10, 0
                ),
                LocalDateTime.of(
                    2026, 9, 21,
                    14, 0
                ),
                true
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
                            true
                        );

                        return null;

                    } catch (Throwable throwable) {
                        return throwable;
                    }
                });

            Future<Throwable> modifyFuture =
                executor.submit(() -> {

                    ready.countDown();
                    start.await();

                    try {
                        shiftService.modify(
                            fixture.workplaceId(),
                            fixture.scheduleId(),
                            fixture.shiftId(),
                            modifyRequest,
                            fixture.managerUserId()
                        );

                        return null;

                    } catch (Throwable throwable) {
                        return throwable;
                    }
                });

            assertThat(
                ready.await(3, TimeUnit.SECONDS)
            ).isTrue();

            start.countDown();

            Throwable publishError =
                publishFuture.get(
                    5,
                    TimeUnit.SECONDS
                );

            Throwable modifyError =
                modifyFuture.get(
                    5,
                    TimeUnit.SECONDS
                );

            assertThat(publishError)
                .isNull();

            Schedule finalSchedule =
                scheduleRepository.findById(
                    fixture.scheduleId()
                ).orElseThrow();

            Shift finalShift =
                shiftRepository.findById(
                    fixture.shiftId()
                ).orElseThrow();

            assertThat(finalSchedule.getStatus())
                .isEqualTo(
                    ScheduleStatus.PUBLISHED
                );

            if (modifyError == null) {

                assertThat(finalShift.getMember().getId())
                    .isEqualTo(
                        fixture.replacementMemberId()
                    );

            } else {

                assertThat(modifyError)
                    .isInstanceOf(
                        BusinessException.class
                    );

                BusinessException businessException =
                    (BusinessException) modifyError;

                assertThat(
                    businessException.getErrorCode()
                ).isEqualTo(
                    ShiftErrorCode.INVALID_STATUS_VALUE
                );

                assertThat(finalShift.getMember().getId())
                    .isEqualTo(
                        fixture.employeeMemberId()
                    );
            }

            var employeeNotifications =
                notificationRepository.findAllByUserId(
                    fixture.employeeUserId()
                );

            var replacementNotifications =
                notificationRepository.findAllByUserId(
                    fixture.replacementUserId()
                );

            Long finalMemberId =
                finalShift.getMember().getId();

            if (finalMemberId.equals(
                fixture.employeeMemberId()
            )) {

                assertThat(employeeNotifications)
                    .hasSize(1);

                assertThat(replacementNotifications)
                    .isEmpty();

            } else {

                assertThat(finalMemberId)
                    .isEqualTo(
                        fixture.replacementMemberId()
                    );

                assertThat(employeeNotifications)
                    .isEmpty();

                assertThat(replacementNotifications)
                    .hasSize(1);
            }

        } finally {
            executor.shutdownNow();
        }
    }

    private Fixture createFixture(String suffix) {

        TransactionTemplate transactionTemplate =
            new TransactionTemplate(transactionManager);

        return transactionTemplate.execute(status -> {

            User managerUser =
                userRepository.save(
                    new User(
                        "manager-" + suffix + "@test.com",
                        "password-hash",
                        "manager"
                    )
                );

            User employeeUser =
                userRepository.save(
                    new User(
                        "employee-" + suffix + "@test.com",
                        "password-hash",
                        "employee"
                    )
                );

            User replacementUser =
                userRepository.save(
                    new User(
                        "replacement-" + suffix + "@test.com",
                        "password-hash",
                        "replacement"
                    )
                );

            Workplace workplace =
                workplaceRepository.save(
                    new Workplace(
                        "동시성 테스트 매장 " + suffix,
                        "INVITE-" + suffix
                    )
                );

            LocalDateTime joinedAt =
                LocalDateTime.of(
                    2026, 9, 1,
                    9, 0
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

            WorkplaceMember replacementMember =
                workplaceMemberRepository.save(
                    new WorkplaceMember(
                        workplace,
                        replacementUser,
                        WorkplaceRole.EMPLOYEE,
                        joinedAt
                    )
                );

            Schedule schedule =
                scheduleRepository.save(
                    new Schedule(
                        workplace,
                        LocalDate.of(2026, 9, 21)
                    )
                );

            Shift shift =
                shiftRepository.save(
                    new Shift(
                        schedule,
                        employeeMember,
                        LocalDateTime.of(
                            2026, 9, 21,
                            10, 0
                        ),
                        LocalDateTime.of(
                            2026, 9, 21,
                            14, 0
                        ),
                        ShiftStatus.SCHEDULED
                    )
                );

            return new Fixture(
                workplace.getId(),
                managerUser.getId(),
                employeeUser.getId(),
                employeeMember.getId(),
                replacementUser.getId(),
                replacementMember.getId(),
                schedule.getId(),
                shift.getId()
            );
        });
    }

    private record Fixture(
        Long workplaceId,
        Long managerUserId,
        Long employeeUserId,
        Long employeeMemberId,
        Long replacementUserId,
        Long replacementMemberId,
        Long scheduleId,
        Long shiftId
    ) {
    }

}
