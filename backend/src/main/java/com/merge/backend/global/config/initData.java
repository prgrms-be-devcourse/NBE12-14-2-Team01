package com.merge.backend.global.config;


import com.merge.backend.domain.notification.entity.Notification;
import com.merge.backend.domain.notification.entity.NotificationType;
import com.merge.backend.domain.notification.repository.NotificationRepository;
import com.merge.backend.domain.shift.entity.RegularShiftPattern;
import com.merge.backend.domain.shift.entity.Schedule;
import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.entity.ShiftStatus;
import com.merge.backend.domain.shift.entity.UnavailableTime;
import com.merge.backend.domain.shift.repository.RegularShiftPatternRepository;
import com.merge.backend.domain.shift.repository.ScheduleRepository;
import com.merge.backend.domain.shift.repository.ShiftRepository;
import com.merge.backend.domain.shift.repository.UnavailableTimeRepository;
import com.merge.backend.domain.substitute.entity.RequestStatus;
import com.merge.backend.domain.substitute.entity.SubstituteCandidate;
import com.merge.backend.domain.substitute.entity.SubstituteRequest;
import com.merge.backend.domain.substitute.repository.SubstituteCandidateRepository;
import com.merge.backend.domain.substitute.repository.SubstituteRequestRepository;
import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.user.repository.UserRepository;
import com.merge.backend.domain.workplace.entity.Workplace;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;
import com.merge.backend.domain.workplace.repository.WorkplaceMemberRepository;
import com.merge.backend.domain.workplace.repository.WorkplaceRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@RequiredArgsConstructor
public class initData {
    private final UserRepository userRepository;
    private final WorkplaceRepository workplaceRepository;
    private final WorkplaceMemberRepository workplaceMemberRepository;
    private final RegularShiftPatternRepository regularShiftPatternRepository;
    private final ScheduleRepository scheduleRepository;
    private final ShiftRepository shiftRepository;
    private final UnavailableTimeRepository unavailableTimeRepository;
    private final SubstituteRequestRepository substituteRequestRepository;
    private final SubstituteCandidateRepository substituteCandidateRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    @Profile({"dev", "local", "prod"})
    @Transactional
    public CommandLineRunner initDummyData() {
        return args -> {

            LocalDateTime now = LocalDateTime.now();
            String defaultPassword = passwordEncoder.encode("1234");

            // 1. User 생성 (매니저 1, 알바생 2)
            User managerUser = userRepository.save(
                new User("manager@test.com", defaultPassword, "김점장")
            );
            User worker1User = userRepository.save(
                new User("worker1@test.com", defaultPassword, "이알바")
            );
            User worker2User = userRepository.save(
                new User("worker2@test.com", defaultPassword, "박알바")
            );

            // 2. Workplace 생성
            Workplace workplace = workplaceRepository.save(
                new Workplace("강남 1호점", "1234")
            );

            // 3. WorkplaceMember 생성
            WorkplaceMember managerMember = workplaceMemberRepository.save(
                new WorkplaceMember(workplace, managerUser, WorkplaceRole.MANAGER, now)
            );
            WorkplaceMember worker1Member = workplaceMemberRepository.save(
                new WorkplaceMember(workplace, worker1User, WorkplaceRole.EMPLOYEE, now)
            );
            WorkplaceMember worker2Member = workplaceMemberRepository.save(
                new WorkplaceMember(workplace, worker2User, WorkplaceRole.EMPLOYEE, now)
            );

            // 4. RegularShiftPattern (고정 근무 패턴 - 총 5개)
            // (1) 이알바: 월요일 09:00 ~ 17:00
            regularShiftPatternRepository.save(
                new RegularShiftPattern(
                    worker1Member,
                    DayOfWeek.MONDAY,
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0)
                )
            );
            // (2) 이알바: 수요일 09:00 ~ 17:00
            regularShiftPatternRepository.save(
                new RegularShiftPattern(
                    worker1Member,
                    DayOfWeek.WEDNESDAY,
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0)
                )
            );
            // (3) 이알바: 금요일 17:00 ~ 22:00
            regularShiftPatternRepository.save(
                new RegularShiftPattern(
                    worker1Member,
                    DayOfWeek.FRIDAY,
                    LocalTime.of(17, 0),
                    LocalTime.of(22, 0)
                )
            );
            // (4) 박알바: 월요일 17:00 ~ 22:00
            regularShiftPatternRepository.save(
                new RegularShiftPattern(
                    worker2Member,
                    DayOfWeek.MONDAY,
                    LocalTime.of(17, 0),
                    LocalTime.of(22, 0)
                )
            );
            // (5) 박알바: 목요일 09:00 ~ 17:00
            regularShiftPatternRepository.save(
                new RegularShiftPattern(
                    worker2Member,
                    DayOfWeek.THURSDAY,
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0)
                )
            );

            // 5. UnavailableTime (근무 불가 시간 - 총 3개)
            // (1) 박알바: 내일 13:00 ~ 18:00
            unavailableTimeRepository.save(
                new UnavailableTime(
                    worker2User,
                    now.plusDays(1).with(LocalTime.of(13, 0)),
                    now.plusDays(1).with(LocalTime.of(18, 0))
                )
            );
            // (2) 이알바: 이번 주 토요일 14:00 ~ 20:00
            unavailableTimeRepository.save(
                new UnavailableTime(
                    worker1User,
                    now.with(DayOfWeek.SATURDAY).with(LocalTime.of(14, 0)),
                    now.with(DayOfWeek.SATURDAY).with(LocalTime.of(20, 0))
                )
            );
            // (3) 박알바: 다음 주 월요일 09:00 ~ 13:00
            unavailableTimeRepository.save(
                new UnavailableTime(
                    worker2User,
                    now.plusWeeks(1).with(DayOfWeek.MONDAY).with(LocalTime.of(9, 0)),
                    now.plusWeeks(1).with(DayOfWeek.MONDAY).with(LocalTime.of(13, 0))
                )
            );

            // 6. Schedule (주간 스케줄) 생성
            LocalDate thisMonday = LocalDate.now().with(DayOfWeek.MONDAY);
            Schedule schedule = new Schedule(workplace, thisMonday);
            schedule.publish(now); // PUBLISHED 상태로 변경
            scheduleRepository.save(schedule);

            // 7. Shift (실제 스케줄 근무) 생성
            Shift shift1 = shiftRepository.save(
                new Shift(
                    schedule,
                    worker1Member,
                    thisMonday.atTime(9, 0),
                    thisMonday.atTime(17, 0),
                    ShiftStatus.SCHEDULED
                )
            );

            Shift shift2 = shiftRepository.save(
                new Shift(
                    schedule,
                    worker2Member,
                    thisMonday.plusDays(1).atTime(9, 0),
                    thisMonday.plusDays(1).atTime(17, 0),
                    ShiftStatus.SCHEDULED
                )
            );

            // 8. SubstituteRequest #1 (이알바 ➔ 박알바 대타 요청, 수락 시나리오)
            SubstituteRequest subRequest1 = substituteRequestRepository.save(
                new SubstituteRequest(shift1, worker1Member, RequestStatus.OPEN)
            );
            SubstituteCandidate candidate1 = substituteCandidateRepository.save(
                new SubstituteCandidate(subRequest1, worker2Member)
            );
            candidate1.acceptRequest(now); // 수락 완료

            notificationRepository.save(
                new Notification(
                    managerMember,
                    NotificationType.SUBSTITUTE_REQUEST_RECEIVED,
                    "이알바 님이 박알바 님에게 대타를 요청했습니다."
                )
            );

            // 9. SubstituteRequest #2 (박알바 ➔ 이알바 대타 요청, 대기중 시나리오)
            SubstituteRequest subRequest2 = substituteRequestRepository.save(
                new SubstituteRequest(shift2, worker2Member, RequestStatus.OPEN)
            );
            substituteCandidateRepository.save(
                new SubstituteCandidate(subRequest2, worker1Member)
            );

            notificationRepository.save(
                new Notification(
                    worker1Member,
                    NotificationType.SUBSTITUTE_REQUEST_RECEIVED,
                    "박알바 님으로부터 대타 요청이 도착했습니다."
                )
            );

            System.out.println("=================================================");
            System.out.println(">>> [DummyDataInit] 초기 더미 데이터 생성이 완료되었습니다.");
            System.out.println("=================================================");
        };
    }
}

