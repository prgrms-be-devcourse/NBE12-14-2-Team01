package com.merge.backend.domain.shift.service;

import com.merge.backend.domain.shift.entity.Schedule;
import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.entity.ShiftStatus;
import com.merge.backend.domain.shift.exception.ScheduleErrorCode;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.merge.backend.domain.shift.event.SchedulePublishedEvent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;

import com.merge.backend.domain.shift.entity.ScheduleStatus;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@SpringBootTest
@ActiveProfiles("test")
@Import(ScheduleRollbackIntegrationTest.FailingEventConfig.class)
class ScheduleRollbackIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkplaceRepository workplaceRepository;

    @Autowired
    private WorkplaceMemberRepository workplaceMemberRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private FailingEventListener failingEventListener;

    @BeforeEach
    void setUp() {
        failingEventListener.setShouldFail(false);
    }

    @Test
    @DisplayName("근무표 공개 실패 시 상태 롤백")
    void t1() {

        // 1. 테스트 데이터 중복 방지
        String uniqueId = java.util.UUID.randomUUID().toString();

        // 2. 관리자와 직원 생성
        User manager = userRepository.save(
            new User("manager-" + uniqueId + "@test.com", "password", "관리자")
        );

        User employee = userRepository.save(
            new User("employee-" + uniqueId + "@test.com", "password", "직원")
        );

        // 3. 근무지 생성
        Workplace workplace = workplaceRepository.save(
            new Workplace("테스트 근무지", uniqueId)
        );

        // 4. 관리자 소속 정보 생성
        workplaceMemberRepository.save(
            new WorkplaceMember(
                workplace,
                manager,
                WorkplaceRole.MANAGER,
                LocalDateTime.now()
            )
        );

        // 5. 직원 소속 정보 생성
        WorkplaceMember employeeMember = workplaceMemberRepository.save(
            new WorkplaceMember(
                workplace,
                employee,
                WorkplaceRole.EMPLOYEE,
                LocalDateTime.now()
            )
        );

        // 6. DRAFT 근무표 생성
        Schedule schedule = scheduleRepository.save(
            new Schedule(
                workplace,
                LocalDate.of(2026, 10, 19)
            )
        );

        // 7. Shift 생성
        shiftRepository.save(
            new Shift(
                schedule,
                employeeMember,
                LocalDateTime.of(2026, 10, 19, 9, 0),
                LocalDateTime.of(2026, 10, 19, 14, 0),
                ShiftStatus.SCHEDULED
            )
        );

        Long scheduleId = schedule.getId();

        // 8. 이벤트 발행 시 강제 오류가 발생하도록 설정
        failingEventListener.setShouldFail(true);

        // 9. 근무표 공개 중 강제 오류가 발생하는지 확인
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> scheduleService.publishSchedule(
                manager.getId(),
                workplace.getId(),
                scheduleId,
                false
            )
        );

        // 10. 우리가 의도한 오류가 맞는지 확인
        assertEquals("테스트용 강제 오류", exception.getMessage());

        // 11. 실제 MySQL에서 근무표 다시 조회
        Schedule result = scheduleRepository.findById(scheduleId)
            .orElseThrow();

        // 12. Rollback 검증
        assertEquals(ScheduleStatus.DRAFT, result.getStatus());
        assertNull(result.getPublishedAt());
    }

    @Test
    @DisplayName("근무표 공개 성공 시 PUBLISHED 상태 저장")
    void t2() {

        // 1. 테스트 데이터 중복 방지
        String uniqueId = java.util.UUID.randomUUID().toString();

        // 2. 관리자와 직원 생성
        User manager = userRepository.save(
            new User("manager-" + uniqueId + "@test.com", "password", "관리자")
        );

        User employee = userRepository.save(
            new User("employee-" + uniqueId + "@test.com", "password", "직원")
        );

        // 3. 근무지 생성
        Workplace workplace = workplaceRepository.save(
            new Workplace("테스트 근무지", uniqueId)
        );

        // 4. 관리자 소속 정보 생성
        workplaceMemberRepository.save(
            new WorkplaceMember(
                workplace,
                manager,
                WorkplaceRole.MANAGER,
                LocalDateTime.now()
            )
        );

        // 5. 직원 소속 정보 생성
        WorkplaceMember employeeMember = workplaceMemberRepository.save(
            new WorkplaceMember(
                workplace,
                employee,
                WorkplaceRole.EMPLOYEE,
                LocalDateTime.now()
            )
        );

        // 6. DRAFT 근무표 생성
        Schedule schedule = scheduleRepository.save(
            new Schedule(
                workplace,
                LocalDate.of(2026, 10, 19)
            )
        );

        // 7. Shift 생성
        shiftRepository.save(
            new Shift(
                schedule,
                employeeMember,
                LocalDateTime.of(2026, 10, 19, 9, 0),
                LocalDateTime.of(2026, 10, 19, 14, 0),
                ShiftStatus.SCHEDULED
            )
        );

        Long scheduleId = schedule.getId();

        // 8. 이벤트 발행 시 오류가 발생하지 않도록 설정
        failingEventListener.setShouldFail(false);

        // 9. 실제 근무표 공개
        scheduleService.publishSchedule(
            manager.getId(),
            workplace.getId(),
            scheduleId,
            false
        );

        // 10. MySQL에서 근무표 다시 조회
        Schedule result = scheduleRepository.findById(scheduleId)
            .orElseThrow();

        // 11. 공개 성공 검증
        assertEquals(ScheduleStatus.PUBLISHED, result.getStatus());
        assertNotNull(result.getPublishedAt());
    }

    @Test
    @DisplayName("이미 공개된 근무표는 다시 공개할 수 없다")
    void t3() {

        String uniqueId = java.util.UUID.randomUUID().toString();

        User manager = userRepository.save(
            new User("manager-" + uniqueId + "@test.com", "password", "관리자")
        );

        User employee = userRepository.save(
            new User("employee-" + uniqueId + "@test.com", "password", "직원")
        );

        Workplace workplace = workplaceRepository.save(
            new Workplace("테스트 근무지", uniqueId)
        );

        workplaceMemberRepository.save(
            new WorkplaceMember(
                workplace,
                manager,
                WorkplaceRole.MANAGER,
                LocalDateTime.now()
            )
        );

        WorkplaceMember employeeMember = workplaceMemberRepository.save(
            new WorkplaceMember(
                workplace,
                employee,
                WorkplaceRole.EMPLOYEE,
                LocalDateTime.now()
            )
        );

        Schedule schedule = scheduleRepository.save(
            new Schedule(
                workplace,
                LocalDate.of(2026, 10, 19)
            )
        );

        shiftRepository.save(
            new Shift(
                schedule,
                employeeMember,
                LocalDateTime.of(2026, 10, 19, 9, 0),
                LocalDateTime.of(2026, 10, 19, 14, 0),
                ShiftStatus.SCHEDULED
            )
        );

        Long scheduleId = schedule.getId();

        // 1. 첫 번째 공개는 정상적으로 성공
        failingEventListener.setShouldFail(false);

        scheduleService.publishSchedule(
            manager.getId(),
            workplace.getId(),
            scheduleId,
            false
        );

        // 첫 공개 시간 저장
        Schedule firstResult = scheduleRepository.findById(scheduleId)
            .orElseThrow();

        LocalDateTime firstPublishedAt = firstResult.getPublishedAt();

        // 2. 같은 근무표를 다시 공개하면 예외가 발생해야 함
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> scheduleService.publishSchedule(
                manager.getId(),
                workplace.getId(),
                scheduleId,
                false
            )
        );

        assertEquals(
            ScheduleErrorCode.SCHEDULE_NOT_DRAFT,
            exception.getErrorCode()
        );

        // 3. DB 상태가 변경되지 않았는지 확인
        Schedule result = scheduleRepository.findById(scheduleId)
            .orElseThrow();

        assertEquals(ScheduleStatus.PUBLISHED, result.getStatus());
        assertEquals(firstPublishedAt, result.getPublishedAt());
    }

    @TestConfiguration
    static class FailingEventConfig {

        @Bean
        FailingEventListener failingEventListener() {
            return new FailingEventListener();
        }
    }

    static class FailingEventListener {

        private boolean shouldFail = false;

        public void setShouldFail(boolean shouldFail) {
            this.shouldFail = shouldFail;
        }

        @EventListener
        public void onSchedulePublished(SchedulePublishedEvent event) {
            if (shouldFail) {
                throw new RuntimeException("테스트용 강제 오류");
            }
        }
    }
}