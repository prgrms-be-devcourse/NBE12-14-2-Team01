
package com.merge.backend.domain.workplace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.workplace.dto.response.MyWorkplaceResponse;
import com.merge.backend.domain.workplace.entity.Workplace;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;
import com.merge.backend.domain.workplace.exception.WorkplaceErrorCode;
import com.merge.backend.domain.workplace.repository.WorkplaceMemberRepository;
import com.merge.backend.domain.workplace.repository.WorkplaceRepository;
import com.merge.backend.global.config.TimeConfig;
import com.merge.backend.global.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.TransientPropertyValueException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE
)
@Import({
    TimeConfig.class,
    WorkplaceService.class,
    WorkplaceMemberService.class
})
class WorkplaceServiceIntegrationTest {

    @Autowired
    private WorkplaceService workplaceService;

    @Autowired
    private WorkplaceRepository workplaceRepository;

    @Autowired
    private WorkplaceMemberRepository workplaceMemberRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("근무지 생성 시 관리자 멤버십도 DB에 저장된다")
    void createWorkplace() {
        User user = entityManager.persist(
            new User("manager@test.com", "password", "관리자")
        );

        WorkplaceMember created =
            workplaceService.createWorkplace("테스트 매장", user);

        Long workplaceId = created.getWorkplace().getId();
        Long memberId = created.getId();

        entityManager.flush();
        entityManager.clear();

        Workplace workplace =
            workplaceRepository.findById(workplaceId).orElseThrow();

        WorkplaceMember member =
            workplaceMemberRepository.findById(memberId).orElseThrow();

        assertThat(workplace.getName()).isEqualTo("테스트 매장");
        assertThat(member.getRole()).isEqualTo(WorkplaceRole.MANAGER);
        assertThat(member.getUser().getId()).isEqualTo(user.getId());
        assertThat(member.getWorkplace().getId()).isEqualTo(workplaceId);
        assertThat(member.getJoinedAt()).isNotNull();
    }

    @Test
    @DisplayName("초대코드로 참여하면 직원 멤버십이 DB에 저장된다")
    void joinWorkplace() {
        User user = entityManager.persist(
            new User("employee@test.com", "password", "직원")
        );

        Workplace workplace = entityManager.persist(
            new Workplace("테스트 매장", "JOIN1234")
        );

        WorkplaceMember joined =
            workplaceService.joinWorkplace("JOIN1234", user);

        Long memberId = joined.getId();

        entityManager.flush();
        entityManager.clear();

        WorkplaceMember member =
            workplaceMemberRepository.findById(memberId).orElseThrow();

        assertThat(member.getUser().getId()).isEqualTo(user.getId());
        assertThat(member.getWorkplace().getId())
            .isEqualTo(workplace.getId());
        assertThat(member.getRole()).isEqualTo(WorkplaceRole.EMPLOYEE);
        assertThat(member.getLeftAt()).isNull();
    }

    @Test
    @DisplayName("이미 참여한 사용자는 중복 참여할 수 없다")
    void rejectDuplicateJoin() {
        User user = entityManager.persist(
            new User("duplicate@test.com", "password", "직원")
        );

        entityManager.persist(
            new Workplace("테스트 매장", "JOIN5678")
        );

        workplaceService.joinWorkplace("JOIN5678", user);
        entityManager.flush();

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> workplaceService.joinWorkplace("JOIN5678", user)
        );

        assertThat(exception.getErrorCode())
            .isEqualTo(WorkplaceErrorCode.ALREADY_JOINED_WORKPLACE);
    }


    @Test
    @DisplayName("퇴사한 관리자는 초대코드를 조회할 수 없다")
    void rejectRetiredManagerAccess() {
        User user = entityManager.persist(
            new User("retired@test.com", "password", "퇴사 관리자")
        );

        Workplace workplace = entityManager.persist(
            new Workplace("테스트 매장", "RETIRE01")
        );

        // 이미 퇴사 처리된 멤버십을 테스트 데이터로 저장
        entityManager.persist(
            new WorkplaceMember(
                workplace,
                user,
                WorkplaceRole.MANAGER,
                LocalDateTime.of(2026, 9, 1, 9, 0),
                LocalDateTime.of(2026, 9, 15, 18, 0)
            )
        );

        Long workplaceId = workplace.getId();
        Long userId = user.getId();

        entityManager.flush();
        entityManager.clear();

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> workplaceService.getInviteCode(
                workplaceId, userId
            )
        );

        assertThat(exception.getErrorCode())
            .isEqualTo(WorkplaceErrorCode.NOT_WORKPLACE_MEMBER);
    }


    @Test
    @DisplayName("다른 근무지의 관리자는 초대코드를 조회할 수 없다")
    void rejectOtherWorkplaceManagerAccess() {
        User managerA = entityManager.persist(
            new User("managerA@test.com", "password", "A 관리자")
        );

        User managerB = entityManager.persist(
            new User("managerB@test.com", "password", "B 관리자")
        );

        Workplace workplaceA = entityManager.persist(
            new Workplace("매장 A", "WORKA001")
        );

        Workplace workplaceB = entityManager.persist(
            new Workplace("매장 B", "WORKB001")
        );

        LocalDateTime joinedAt =
            LocalDateTime.of(2026, 9, 1, 9, 0);

        entityManager.persist(
            new WorkplaceMember(
                workplaceA, managerA,
                WorkplaceRole.MANAGER, joinedAt
            )
        );

        entityManager.persist(
            new WorkplaceMember(
                workplaceB, managerB,
                WorkplaceRole.MANAGER, joinedAt
            )
        );

        Long managerAId = managerA.getId();
        Long workplaceBId = workplaceB.getId();

        entityManager.flush();
        entityManager.clear();

        // A 매장 관리자가 B 매장의 초대코드에 접근
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> workplaceService.getInviteCode(
                workplaceBId, managerAId
            )
        );

        assertThat(exception.getErrorCode())
            .isEqualTo(WorkplaceErrorCode.NOT_WORKPLACE_MEMBER);
    }


    @Test
    @DisplayName("DB 재조회 후에도 현재 소속 근무지 정보를 DTO로 변환할 수 있다")
    void getMyWorkplacesAfterReload() {
        User user = entityManager.persist(
            new User("reload@test.com", "password", "직원")
        );

        Workplace active = entityManager.persist(
            new Workplace("현재 매장", "ACTIVE01")
        );

        Workplace retired = entityManager.persist(
            new Workplace("퇴사 매장", "RETIRED1")
        );

        LocalDateTime joinedAt =
            LocalDateTime.of(2026, 9, 1, 9, 0);

        entityManager.persist(
            new WorkplaceMember(
                active, user, WorkplaceRole.MANAGER, joinedAt
            )
        );

        entityManager.persist(
            new WorkplaceMember(
                retired,
                user,
                WorkplaceRole.EMPLOYEE,
                joinedAt,
                joinedAt.plusDays(10)
            )
        );

        Long activeId = active.getId();

        entityManager.flush();
        entityManager.clear();

        // 실제 Service와 Repository를 통해 다시 조회
        List<WorkplaceMember> members =
            workplaceService.getMyWorkplaces(user);

        // 영속성 컨텍스트를 비워 프록시 의존 문제 확인
        entityManager.clear();

        assertThat(members).hasSize(1);

        MyWorkplaceResponse response =
            MyWorkplaceResponse.from(members.get(0));

        assertThat(response.workplaceId()).isEqualTo(activeId);
        assertThat(response.name()).isEqualTo("현재 매장");
        assertThat(response.role()).isEqualTo(WorkplaceRole.MANAGER);
    }

    @Test
    @DisplayName("DB 재조회 후에도 현재 구성원의 User 정보를 정상 조회할 수 있다")
    void getWorkplaceMembersAfterReload() {
        User manager = entityManager.persist(
            new User("manager-load@test.com", "password", "관리자")
        );

        User employee = entityManager.persist(
            new User("employee-load@test.com", "password", "홍길동")
        );

        User retired = entityManager.persist(
            new User("retired-load@test.com", "password", "퇴사자")
        );

        Workplace workplace = entityManager.persist(
            new Workplace("조회 테스트 매장", "MEMBER01")
        );

        LocalDateTime joinedAt =
            LocalDateTime.of(2026, 9, 1, 9, 0);

        // 재직 중인 관리자
        entityManager.persist(
            new WorkplaceMember(
                workplace, manager, WorkplaceRole.MANAGER, joinedAt
            )
        );

        // 재직 중인 직원
        entityManager.persist(
            new WorkplaceMember(
                workplace, employee, WorkplaceRole.EMPLOYEE, joinedAt
            )
        );

        // 퇴사한 직원
        entityManager.persist(
            new WorkplaceMember(
                workplace,
                retired,
                WorkplaceRole.EMPLOYEE,
                joinedAt,
                joinedAt.plusDays(10)
            )
        );

        Long workplaceId = workplace.getId();
        Long managerId = manager.getId();

        entityManager.flush();
        entityManager.clear();

        // 실제 관리자 권한 검증 + 구성원 조회
        List<WorkplaceMember> members =
            workplaceService.getWorkplaceMembers(
                workplaceId, managerId
            );

        // 조회한 엔티티를 영속성 컨텍스트에서 분리
        entityManager.clear();

        // 퇴사자는 제외되고 재직자 2명만 조회
        assertThat(members).hasSize(2);

        // User 연관관계를 통해 이메일을 정상적으로 읽는지 검증
        assertThat(members)
            .extracting(member -> member.getUser().getEmail())
            .containsExactlyInAnyOrder(
                "manager-load@test.com",
                "employee-load@test.com"
            );

        // 직원의 이름과 역할도 유지되는지 검증
        assertThat(members)
            .filteredOn(member ->
                member.getUser().getEmail()
                    .equals("employee-load@test.com")
            )
            .singleElement()
            .satisfies(member -> {
                assertThat(member.getUser().getName())
                    .isEqualTo("홍길동");

                assertThat(member.getRole())
                    .isEqualTo(WorkplaceRole.EMPLOYEE);
            });
    }


    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("관리자 멤버십 저장 실패 시 근무지 생성도 롤백된다")
    void rollbackWorkplaceCreation() {
        String workplaceName = "롤백 검증 매장";
        long beforeCount = workplaceRepository.count();

        User unsavedUser = new User(
            "rollback@test.com",
            "password",
            "테스트 관리자"
        );

        // 멤버십 저장 실패 시 정확한 예외와 원인 검증
        assertThatThrownBy(
            () -> workplaceService.createWorkplace(
                workplaceName,
                unsavedUser
            )
        )
            .isInstanceOf(InvalidDataAccessApiUsageException.class)
            .hasRootCauseInstanceOf(TransientPropertyValueException.class);

        // 실패 이후 DB에 근무지가 남아 있지 않아야 한다
        assertThat(workplaceRepository.count())
            .isEqualTo(beforeCount);

        assertThat(workplaceRepository.findAll())
            .noneMatch(workplace ->
                workplaceName.equals(workplace.getName())
            );
    }



}
