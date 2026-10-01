package com.merge.backend.global.config;


import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.user.repository.UserRepository;
import com.merge.backend.domain.workplace.entity.Workplace;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;
import com.merge.backend.domain.workplace.repository.WorkplaceMemberRepository;
import com.merge.backend.domain.workplace.repository.WorkplaceRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@RequiredArgsConstructor
public class InitData {

    private final InitDataService initDataService;

    @Bean
    // 필요 시 'init-data' 프로필을 켜서 초기화
    @Profile({"dev", "local", "init-data"})
    public CommandLineRunner initDummyData() {
        return args -> initDataService.init();
    }
    // 트랜잭션 적용을 위해 내부 서비스 클래스로 분리
    @Component
    @RequiredArgsConstructor
    public static class InitDataService {

        private final UserRepository userRepository;
        private final WorkplaceRepository workplaceRepository;
        private final WorkplaceMemberRepository workplaceMemberRepository;
        private final PasswordEncoder passwordEncoder;

        @Transactional
        public void init() {
            // 이미 데이터가 존재하는지 확인하여 중복 생성 방지
            if (userRepository.count() > 0) {
                System.out.println("이미 데이터가 존재하므로 초기화 작업을 스킵합니다.");
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            String defaultPassword = passwordEncoder.encode("1234");

            // User 생성 (매니저 1명, 직원 2명)
            User managerUser = userRepository.save(
                new User("manager@test.com", defaultPassword, "김점장")
            );
            User worker1User = userRepository.save(
                new User("worker1@test.com", defaultPassword, "이알바")
            );
            User worker2User = userRepository.save(
                new User("worker2@test.com", defaultPassword, "박알바")
            );

            // Workplace 생성
            Workplace workplace = workplaceRepository.save(
                new Workplace("강남 1호점", "1234")
            );

            // WorkplaceMember 생성
            workplaceMemberRepository.save(
                new WorkplaceMember(workplace, managerUser, WorkplaceRole.MANAGER, now)
            );
            workplaceMemberRepository.save(
                new WorkplaceMember(workplace, worker1User, WorkplaceRole.EMPLOYEE, now)
            );
            workplaceMemberRepository.save(
                new WorkplaceMember(workplace, worker2User, WorkplaceRole.EMPLOYEE, now)
            );

            System.out.println("=================================================");
            System.out.println(">>> [InitData] 최소 초기 더미 데이터 생성이 완료되었습니다.");
            System.out.println("=================================================");
        }
    }
}

