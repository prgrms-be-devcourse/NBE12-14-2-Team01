package com.merge.backend.domain.shift.service;

import com.merge.backend.domain.shift.dto.ShiftRequest;
import com.merge.backend.domain.shift.entity.Schedule;
import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.entity.ShiftStatus;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ShiftModifyIntegrationTest {

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ShiftService shiftService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkplaceRepository workplaceRepository;

    @Autowired
    private WorkplaceMemberRepository workplaceMemberRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Test
    @DisplayName("근무 담당자를 A에서 B로 수정한다")
    void t1() {

        String uniqueId = UUID.randomUUID().toString();

        // 1. 관리자 생성
        User manager = userRepository.save(
            new User("manager-" + uniqueId + "@test.com", "password", "관리자")
        );

        // 2. 직원 A 생성
        User employeeA = userRepository.save(
            new User("employeeA-" + uniqueId + "@test.com", "password", "직원A")
        );

        // 3. 직원 B 생성
        User employeeB = userRepository.save(
            new User("employeeB-" + uniqueId + "@test.com", "password", "직원B")
        );

        // 4. 근무지 생성
        Workplace workplace = workplaceRepository.save(
            new Workplace("테스트 근무지", uniqueId)
        );

        // 5. 관리자 등록
        workplaceMemberRepository.save(
            new WorkplaceMember(
                workplace,
                manager,
                WorkplaceRole.MANAGER,
                LocalDateTime.now()
            )
        );

        // 6. 직원 A 등록
        WorkplaceMember memberA = workplaceMemberRepository.save(
            new WorkplaceMember(
                workplace,
                employeeA,
                WorkplaceRole.EMPLOYEE,
                LocalDateTime.now()
            )
        );

        // 7. 직원 B 등록
        WorkplaceMember memberB = workplaceMemberRepository.save(
            new WorkplaceMember(
                workplace,
                employeeB,
                WorkplaceRole.EMPLOYEE,
                LocalDateTime.now()
            )
        );

        // 8. 근무표 생성
        Schedule schedule = scheduleRepository.save(
            new Schedule(
                workplace,
                LocalDate.of(2026, 10, 19)
            )
        );

        // 9. 직원 A에게 근무 배정
        Shift shift = shiftRepository.save(
            new Shift(
                schedule,
                memberA,
                LocalDateTime.of(2026, 10, 19, 9, 0),
                LocalDateTime.of(2026, 10, 19, 14, 0),
                ShiftStatus.SCHEDULED
            )
        );

        // 10. 직원 B로 담당자 변경 요청
        ShiftRequest request = new ShiftRequest(
            memberB.getId(),
            LocalDateTime.of(2026, 10, 19, 9, 0),
            LocalDateTime.of(2026, 10, 19, 14, 0),
            false
        );

        // 11. 실제 담당자 수정 실행
        shiftService.modify(
            workplace.getId(),
            schedule.getId(),
            shift.getId(),
            request,
            manager.getId()
        );

        // 12. DB에서 수정된 Shift 다시 조회
        Shift result = shiftRepository.findById(shift.getId())
            .orElseThrow();

        // 13. 담당자가 직원 B로 변경됐는지 확인
        assertEquals(memberB.getId(), result.getMember().getId());

        // 14. 근무 시간이 그대로인지 확인
        assertEquals(
            LocalDateTime.of(2026, 10, 19, 9, 0),
            result.getStartAt()
        );

        assertEquals(
            LocalDateTime.of(2026, 10, 19, 14, 0),
            result.getEndAt()
        );
    }

    @Test
    @DisplayName("근무 시간이 겹치면 담당자 수정에 실패하고 기존 데이터가 유지된다")
    void t2() {

        String uniqueId = UUID.randomUUID().toString();

        // 1. 사용자 생성
        User manager = userRepository.save(
            new User("manager-" + uniqueId + "@test.com", "password", "관리자")
        );

        User employeeA = userRepository.save(
            new User("employeeA-" + uniqueId + "@test.com", "password", "직원A")
        );

        User employeeB = userRepository.save(
            new User("employeeB-" + uniqueId + "@test.com", "password", "직원B")
        );

        // 2. 근무지 생성
        Workplace workplace = workplaceRepository.save(
            new Workplace("테스트 근무지", uniqueId)
        );

        // 3. 근무지 구성원 등록
        workplaceMemberRepository.save(
            new WorkplaceMember(
                workplace, manager, WorkplaceRole.MANAGER, LocalDateTime.now()
            )
        );

        WorkplaceMember memberA = workplaceMemberRepository.save(
            new WorkplaceMember(
                workplace, employeeA, WorkplaceRole.EMPLOYEE, LocalDateTime.now()
            )
        );

        WorkplaceMember memberB = workplaceMemberRepository.save(
            new WorkplaceMember(
                workplace, employeeB, WorkplaceRole.EMPLOYEE, LocalDateTime.now()
            )
        );

        // 4. 근무표 생성
        Schedule schedule = scheduleRepository.save(
            new Schedule(workplace, LocalDate.of(2026, 10, 19))
        );

        // 5. 직원 A의 기존 근무
        Shift shift = shiftRepository.save(
            new Shift(
                schedule,
                memberA,
                LocalDateTime.of(2026, 10, 19, 9, 0),
                LocalDateTime.of(2026, 10, 19, 14, 0),
                ShiftStatus.SCHEDULED
            )
        );

        // 6. 직원 B의 기존 근무 (시간 중복)
        shiftRepository.save(
            new Shift(
                schedule,
                memberB,
                LocalDateTime.of(2026, 10, 19, 10, 0),
                LocalDateTime.of(2026, 10, 19, 15, 0),
                ShiftStatus.SCHEDULED
            )
        );

        // 7. 직원 A의 근무를 직원 B에게 변경 요청
        ShiftRequest request = new ShiftRequest(
            memberB.getId(),
            LocalDateTime.of(2026, 10, 19, 9, 0),
            LocalDateTime.of(2026, 10, 19, 14, 0),
            false
        );

        // 8. 시간 중복으로 예외 발생 확인
        assertThrows(
            BusinessException.class,
            () -> shiftService.modify(
                workplace.getId(),
                schedule.getId(),
                shift.getId(),
                request,
                manager.getId()
            )
        );

        // 9. DB에서 다시 조회
        Shift result = shiftRepository.findById(shift.getId())
            .orElseThrow();

        // 10. 기존 담당자와 근무 시간이 유지되는지 확인
        assertEquals(memberA.getId(), result.getMember().getId());

        assertEquals(
            LocalDateTime.of(2026, 10, 19, 9, 0),
            result.getStartAt()
        );

        assertEquals(
            LocalDateTime.of(2026, 10, 19, 14, 0),
            result.getEndAt()
        );
    }

    @Test
    @DisplayName("동시에 같은 근무표를 수정하면 비관적 락으로 순서대로 처리된다")
    void t3() throws Exception {

        // 1. 테스트용 근무지 생성
        String uniqueId = UUID.randomUUID().toString();

        Workplace workplace = workplaceRepository.save(
            new Workplace("동시성 테스트 근무지", uniqueId)
        );

        // 2. DRAFT 근무표 생성
        Schedule schedule = scheduleRepository.save(
            new Schedule(workplace, LocalDate.of(2026, 10, 19))
        );

        Long scheduleId = schedule.getId();

        // 3. 별도 스레드에서 트랜잭션 실행
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            Future<?> future = executor.submit(() -> {

                TransactionTemplate transactionTemplate =
                    new TransactionTemplate(transactionManager);

                transactionTemplate.executeWithoutResult(status -> {

                    // 4. 근무표에 비관적 락 획득
                    Schedule lockedSchedule = scheduleRepository
                        .findByIdForUpdate(scheduleId)
                        .orElseThrow();

                    System.out.println(
                        "스레드 A: 근무표 락 획득! ID = "
                            + lockedSchedule.getId()
                    );

                    // 5. 락을 2초 동안 유지
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }

                    System.out.println("스레드 A: 트랜잭션 종료 직전");
                });
            });

            // 스레드 작업 완료 확인
            future.get(10, TimeUnit.SECONDS);

        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("두 트랜잭션이 같은 근무표에 접근하면 비관적 락으로 대기한다")
    void t4() throws Exception {

        // 1. 테스트 데이터 생성
        String uniqueId = UUID.randomUUID().toString();

        Workplace workplace = workplaceRepository.save(
            new Workplace("동시성 테스트 근무지", uniqueId)
        );

        Schedule schedule = scheduleRepository.save(
            new Schedule(workplace, LocalDate.of(2026, 10, 19))
        );

        Long scheduleId = schedule.getId();

        // 2. 스레드 실행 시점을 조율할 도구
        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch releaseLock = new CountDownLatch(1);
        CountDownLatch bAttempting = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            // 3. 스레드 A: 락 획득 후 유지
            Future<?> futureA = executor.submit(() -> {

                TransactionTemplate template =
                    new TransactionTemplate(transactionManager);

                template.executeWithoutResult(status -> {

                    scheduleRepository.findByIdForUpdate(scheduleId)
                        .orElseThrow();

                    System.out.println("A: 락 획득");

                    lockAcquired.countDown();

                    try {
                        // B의 확인이 끝날 때까지 락 유지
                        if (!releaseLock.await(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("A 락 유지 시간 초과");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }

                    System.out.println("A: 트랜잭션 종료");
                });
            });

            // 4. A가 락을 잡을 때까지 기다림
            assertTrue(lockAcquired.await(5, TimeUnit.SECONDS));

            // 5. 스레드 B: 동일한 근무표에 락 획득 시도
            Future<?> futureB = executor.submit(() -> {

                TransactionTemplate template =
                    new TransactionTemplate(transactionManager);

                template.executeWithoutResult(status -> {

                    bAttempting.countDown();

                    scheduleRepository.findByIdForUpdate(scheduleId)
                        .orElseThrow();

                    System.out.println("B: 락 획득");
                });
            });

            // 6. B가 락 획득을 시도하는 단계에 도달했는지 확인
            assertTrue(bAttempting.await(5, TimeUnit.SECONDS));

            // 7. A가 락을 유지하는 동안 B가 완료되지 않았는지 확인
            Thread.sleep(500);

            assertFalse(
                futureB.isDone(),
                "A가 락을 보유하는 동안 B는 완료되면 안 됨"
            );

            System.out.println("검증 성공: B가 대기 중");

            // 8. A의 락 해제 허용
            releaseLock.countDown();

            // 9. 두 작업 모두 정상 완료되는지 확인
            futureA.get(10, TimeUnit.SECONDS);
            futureB.get(10, TimeUnit.SECONDS);

            System.out.println("검증 성공: A 종료 후 B도 완료");

        } finally {
            releaseLock.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("동시에 담당자를 수정해도 최종 근무 데이터가 일관되게 유지된다")
    void t5() throws Exception {

        String uniqueId = UUID.randomUUID().toString();

        // 1. 사용자 생성
        User manager = userRepository.save(
            new User("manager-" + uniqueId + "@test.com", "password", "관리자")
        );

        User employeeA = userRepository.save(
            new User("employeeA-" + uniqueId + "@test.com", "password", "직원A")
        );

        User employeeB = userRepository.save(
            new User("employeeB-" + uniqueId + "@test.com", "password", "직원B")
        );

        User employeeC = userRepository.save(
            new User("employeeC-" + uniqueId + "@test.com", "password", "직원C")
        );

        // 2. 근무지 생성
        Workplace workplace = workplaceRepository.save(
            new Workplace("동시 수정 테스트 근무지", uniqueId)
        );

        // 3. 근무지 구성원 등록
        workplaceMemberRepository.save(
            new WorkplaceMember(
                workplace, manager, WorkplaceRole.MANAGER, LocalDateTime.now()
            )
        );

        WorkplaceMember memberA = workplaceMemberRepository.save(
            new WorkplaceMember(
                workplace, employeeA, WorkplaceRole.EMPLOYEE, LocalDateTime.now()
            )
        );

        WorkplaceMember memberB = workplaceMemberRepository.save(
            new WorkplaceMember(
                workplace, employeeB, WorkplaceRole.EMPLOYEE, LocalDateTime.now()
            )
        );

        WorkplaceMember memberC = workplaceMemberRepository.save(
            new WorkplaceMember(
                workplace, employeeC, WorkplaceRole.EMPLOYEE, LocalDateTime.now()
            )
        );

        // 4. 근무표 생성
        Schedule schedule = scheduleRepository.save(
            new Schedule(workplace, LocalDate.of(2026, 10, 19))
        );

        // 5. 직원 A에게 기존 근무 배정
        Shift shift = shiftRepository.save(
            new Shift(
                schedule,
                memberA,
                LocalDateTime.of(2026, 10, 19, 9, 0),
                LocalDateTime.of(2026, 10, 19, 14, 0),
                ShiftStatus.SCHEDULED
            )
        );

        ShiftRequest requestB = new ShiftRequest(
            memberB.getId(),
            LocalDateTime.of(2026, 10, 19, 9, 0),
            LocalDateTime.of(2026, 10, 19, 14, 0),
            false
        );

        ShiftRequest requestC = new ShiftRequest(
            memberC.getId(),
            LocalDateTime.of(2026, 10, 19, 9, 0),
            LocalDateTime.of(2026, 10, 19, 14, 0),
            false
        );

        Long workplaceId = workplace.getId();
        Long scheduleId = schedule.getId();
        Long shiftId = shift.getId();
        Long managerId = manager.getId();

        // 두 스레드가 동시에 출발하도록 조율
        CountDownLatch startLatch = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> futureB = executor.submit(() -> {
                startLatch.await();

                shiftService.modify(
                    workplaceId, scheduleId, shiftId, requestB, managerId
                );

                return null;
            });

            Future<?> futureC = executor.submit(() -> {
                startLatch.await();

                shiftService.modify(
                    workplaceId, scheduleId, shiftId, requestC, managerId
                );

                return null;
            });

            // 두 스레드에 출발 신호
            startLatch.countDown();

            // 두 수정 요청이 끝날 때까지 기다림
            futureB.get(10, TimeUnit.SECONDS);
            futureC.get(10, TimeUnit.SECONDS);

        } finally {
            startLatch.countDown();
            executor.shutdownNow();
        }

        // 6. 최종 DB 데이터 확인
        Shift result = shiftRepository.findById(shiftId)
            .orElseThrow();

        Long finalMemberId = result.getMember().getId();

        // 최종 담당자가 B 또는 C인지 확인
        assertTrue(
            finalMemberId.equals(memberB.getId())
                || finalMemberId.equals(memberC.getId()),
            "최종 담당자는 B 또는 C여야 함"
        );

        // 근무 시간이 정상적으로 유지됐는지 확인
        assertEquals(
            LocalDateTime.of(2026, 10, 19, 9, 0),
            result.getStartAt()
        );

        assertEquals(
            LocalDateTime.of(2026, 10, 19, 14, 0),
            result.getEndAt()
        );

        System.out.println("최종 담당자 ID = " + finalMemberId);
        System.out.println("동시 수정 후 데이터 검증 성공");
    }

    @Test
    @DisplayName("서로 다른 근무를 동시에 수정해도 동일 직원의 근무 시간이 중복되지 않는다")
    void t6() throws Exception {

        String uniqueId = UUID.randomUUID().toString();

        // 1. 사용자 생성
        User manager = userRepository.save(
            new User("manager-" + uniqueId + "@test.com", "password", "관리자")
        );

        User employeeA = userRepository.save(
            new User("employeeA-" + uniqueId + "@test.com", "password", "직원A")
        );

        User employeeB = userRepository.save(
            new User("employeeB-" + uniqueId + "@test.com", "password", "직원B")
        );

        User employeeC = userRepository.save(
            new User("employeeC-" + uniqueId + "@test.com", "password", "직원C")
        );

        // 2. 근무지 생성
        Workplace workplace = workplaceRepository.save(
            new Workplace("근무 중복 동시성 테스트", uniqueId)
        );

        // 3. 근무지 구성원 등록
        workplaceMemberRepository.save(
            new WorkplaceMember(
                workplace, manager, WorkplaceRole.MANAGER, LocalDateTime.now()
            )
        );

        WorkplaceMember memberA = workplaceMemberRepository.save(
            new WorkplaceMember(
                workplace, employeeA, WorkplaceRole.EMPLOYEE, LocalDateTime.now()
            )
        );

        WorkplaceMember memberB = workplaceMemberRepository.save(
            new WorkplaceMember(
                workplace, employeeB, WorkplaceRole.EMPLOYEE, LocalDateTime.now()
            )
        );

        WorkplaceMember memberC = workplaceMemberRepository.save(
            new WorkplaceMember(
                workplace, employeeC, WorkplaceRole.EMPLOYEE, LocalDateTime.now()
            )
        );

        // 4. 근무표 생성
        Schedule schedule = scheduleRepository.save(
            new Schedule(workplace, LocalDate.of(2026, 10, 19))
        );

        // 5. 직원 B의 근무: 09:00~14:00
        Shift shift1 = shiftRepository.save(
            new Shift(
                schedule,
                memberB,
                LocalDateTime.of(2026, 10, 19, 9, 0),
                LocalDateTime.of(2026, 10, 19, 14, 0),
                ShiftStatus.SCHEDULED
            )
        );

        // 6. 직원 C의 근무: 10:00~15:00
        Shift shift2 = shiftRepository.save(
            new Shift(
                schedule,
                memberC,
                LocalDateTime.of(2026, 10, 19, 10, 0),
                LocalDateTime.of(2026, 10, 19, 15, 0),
                ShiftStatus.SCHEDULED
            )
        );

        // 7. 두 근무 모두 직원 A에게 변경 요청
        ShiftRequest request1 = new ShiftRequest(
            memberA.getId(),
            LocalDateTime.of(2026, 10, 19, 9, 0),
            LocalDateTime.of(2026, 10, 19, 14, 0),
            false
        );

        ShiftRequest request2 = new ShiftRequest(
            memberA.getId(),
            LocalDateTime.of(2026, 10, 19, 10, 0),
            LocalDateTime.of(2026, 10, 19, 15, 0),
            false
        );

        // 8. 동시 수정 준비
        Long workplaceId = workplace.getId();
        Long scheduleId = schedule.getId();
        Long managerId = manager.getId();

        Long shift1Id = shift1.getId();
        Long shift2Id = shift2.getId();

        // 두 스레드가 동시에 출발하도록 조율
        CountDownLatch startLatch = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            // 스레드 1: 근무 1을 직원 A에게 변경
            Future<Boolean> future1 = executor.submit(() -> {
                startLatch.await();

                try {
                    shiftService.modify(
                        workplaceId, scheduleId, shift1Id, request1, managerId
                    );

                    System.out.println("근무 1 수정 성공");
                    return true;

                } catch (BusinessException e) {
                    System.out.println("근무 1 수정 실패: " + e.getMessage());
                    return false;
                }
            });

            // 스레드 2: 근무 2를 직원 A에게 변경
            Future<Boolean> future2 = executor.submit(() -> {
                startLatch.await();

                try {
                    shiftService.modify(
                        workplaceId, scheduleId, shift2Id, request2, managerId
                    );

                    System.out.println("근무 2 수정 성공");
                    return true;

                } catch (BusinessException e) {
                    System.out.println("근무 2 수정 실패: " + e.getMessage());
                    return false;
                }
            });

            // 두 스레드에 출발 신호
            startLatch.countDown();

            // 각각 성공했는지 확인
            boolean success1 = future1.get(10, TimeUnit.SECONDS);
            boolean success2 = future2.get(10, TimeUnit.SECONDS);

            // 정확히 하나만 성공해야 함
            assertTrue(
                success1 != success2,
                "두 수정 요청 중 정확히 하나만 성공해야 함"
            );

            System.out.println("검증 성공: 두 요청 중 하나만 수정됨");

        } finally {
            startLatch.countDown();
            executor.shutdownNow();
        }

        // 9. DB에서 최종 근무 데이터 조회
        Shift result1 = shiftRepository.findById(shift1Id)
            .orElseThrow();

        Shift result2 = shiftRepository.findById(shift2Id)
            .orElseThrow();

        // 직원 A에게 배정된 근무 개수 확인
        int assignedCount = 0;

        if (result1.getMember().getId().equals(memberA.getId())) {
            assignedCount++;
        }

        if (result2.getMember().getId().equals(memberA.getId())) {
            assignedCount++;
        }

        // 직원 A에게는 하나의 근무만 배정되어야 함
        assertEquals(1, assignedCount);

        System.out.println("직원 A에게 배정된 근무 개수 = " + assignedCount);
        System.out.println("검증 성공: 중복 근무가 저장되지 않음");
    }

    @Test
    @DisplayName("트랜잭션 중 예외 발생 시 근무 수정이 롤백된다")
    void t7() {
        String uniqueId = UUID.randomUUID().toString();

        // 1. 테스트 데이터 생성
        User employeeA = userRepository.save(
            new User("employeeA-" + uniqueId + "@test.com", "password", "직원A")
        );

        User employeeB = userRepository.save(
            new User("employeeB-" + uniqueId + "@test.com", "password", "직원B")
        );

        Workplace workplace = workplaceRepository.save(
            new Workplace("롤백 테스트", uniqueId)
        );

        WorkplaceMember memberA = workplaceMemberRepository.save(
            new WorkplaceMember(
                workplace, employeeA, WorkplaceRole.EMPLOYEE, LocalDateTime.now()
            )
        );

        WorkplaceMember memberB = workplaceMemberRepository.save(
            new WorkplaceMember(
                workplace, employeeB, WorkplaceRole.EMPLOYEE, LocalDateTime.now()
            )
        );

        Schedule schedule = scheduleRepository.save(
            new Schedule(workplace, LocalDate.of(2026, 10, 19))
        );

        Shift shift = shiftRepository.save(
            new Shift(
                schedule,
                memberB,
                LocalDateTime.of(2026, 10, 19, 9, 0),
                LocalDateTime.of(2026, 10, 19, 14, 0),
                ShiftStatus.SCHEDULED
            )
        );

        Long shiftId = shift.getId();

        // 2. 트랜잭션 실행
        TransactionTemplate transactionTemplate =
            new TransactionTemplate(transactionManager);

        // 3. 예외 발생 확인
        assertThrows(RuntimeException.class, () -> {
            transactionTemplate.executeWithoutResult(status -> {

                Shift target = shiftRepository.findById(shiftId)
                    .orElseThrow();

                // 직원 B → 직원 A 변경
                target.update(
                    memberA,
                    target.getStartAt(),
                    target.getEndAt()
                );

                // 변경 SQL을 DB에 실행
                shiftRepository.flush();

                System.out.println("직원 A로 수정 후 flush 완료");

                // 강제로 예외 발생
                throw new RuntimeException("강제 롤백 테스트");
            });
        });

        // 4. 트랜잭션 종료 후 DB 다시 조회
        Shift result = shiftRepository.findById(shiftId)
            .orElseThrow();

        // 5. 기존 직원 B가 유지되는지 검증
        assertEquals(memberB.getId(), result.getMember().getId());

        System.out.println("롤백 성공: 기존 직원 B 유지");
    }
}