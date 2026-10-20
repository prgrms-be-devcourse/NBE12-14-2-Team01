
package com.merge.backend.domain.workplace.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.workplace.entity.Workplace;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;
import com.merge.backend.global.config.TimeConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE
)
@Import(TimeConfig.class)
class WorkplaceMemberRepositoryTest {

    @Autowired
    private WorkplaceMemberRepository workplaceMemberRepository;

    @Autowired
    private TestEntityManager entityManager;

    private static final LocalDateTime JOINED_AT =
        LocalDateTime.of(2026, 9, 1, 9, 0);

    private static final LocalDateTime LEFT_AT =
        LocalDateTime.of(2026, 9, 15, 18, 0);

    @Test
    @DisplayName("사용자의 재직 중인 근무지만 조회한다")
    void findOnlyCurrentWorkplaces() {
        User user = createUser("user1@test.com", "직원1");

        Workplace active1 =
            createWorkplace("매장A", "TESTA001");
        Workplace active2 =
            createWorkplace("매장B", "TESTB001");
        Workplace retired =
            createWorkplace("매장C", "TESTC001");

        createMember(active1, user, WorkplaceRole.EMPLOYEE, null);
        createMember(active2, user, WorkplaceRole.MANAGER, null);
        createMember(retired, user, WorkplaceRole.EMPLOYEE, LEFT_AT);

        entityManager.flush();
        entityManager.clear();

        List<WorkplaceMember> result =
            workplaceMemberRepository
                .findAllByUser_IdAndLeftAtIsNull(user.getId());

        assertThat(result)
            .extracting(member -> member.getWorkplace().getId())
            .containsExactlyInAnyOrder(
                active1.getId(),
                active2.getId()
            );
    }

    @Test
    @DisplayName("근무지의 현재 재직자만 조회한다")
    void findOnlyCurrentMembers() {
        Workplace workplace =
            createWorkplace("매장A", "TESTA002");

        Workplace otherWorkplace =
            createWorkplace("매장B", "TESTB002");

        User manager = createUser("manager2@test.com", "관리자");
        User employee = createUser("employee2@test.com", "직원");
        User retired = createUser("retired2@test.com", "퇴사자");
        User outsider = createUser("outsider2@test.com", "외부직원");

        createMember(
            workplace, manager, WorkplaceRole.MANAGER, null
        );
        createMember(
            workplace, employee, WorkplaceRole.EMPLOYEE, null
        );
        createMember(
            workplace, retired, WorkplaceRole.EMPLOYEE, LEFT_AT
        );
        createMember(
            otherWorkplace, outsider, WorkplaceRole.EMPLOYEE, null
        );

        entityManager.flush();
        entityManager.clear();

        List<WorkplaceMember> result =
            workplaceMemberRepository
                .findAllByWorkplace_IdAndLeftAtIsNull(
                    workplace.getId()
                );

        assertThat(result)
            .extracting(member -> member.getUser().getId())
            .containsExactlyInAnyOrder(
                manager.getId(),
                employee.getId()
            );
    }

    @Test
    @DisplayName("현재 재직 중인 근무지의 멤버십만 조회한다")
    void findActiveMembership() {
        Workplace workplace =
            createWorkplace("매장A", "TESTA003");

        Workplace otherWorkplace =
            createWorkplace("매장B", "TESTB003");

        User manager = createUser("manager3@test.com", "관리자");
        User retired = createUser("retired3@test.com", "퇴사자");

        WorkplaceMember activeMember = createMember(
            workplace, manager, WorkplaceRole.MANAGER, null
        );

        createMember(
            workplace, retired, WorkplaceRole.MANAGER, LEFT_AT
        );

        entityManager.flush();
        entityManager.clear();

        assertThat(
            workplaceMemberRepository
                .findByWorkplace_IdAndUser_IdAndLeftAtIsNull(
                    workplace.getId(),
                    manager.getId()
                )
        )
            .isPresent()
            .get()
            .extracting(WorkplaceMember::getId)
            .isEqualTo(activeMember.getId());

        assertThat(
            workplaceMemberRepository
                .findByWorkplace_IdAndUser_IdAndLeftAtIsNull(
                    workplace.getId(),
                    retired.getId()
                )
        ).isEmpty();

        assertThat(
            workplaceMemberRepository
                .findByWorkplace_IdAndUser_IdAndLeftAtIsNull(
                    otherWorkplace.getId(),
                    manager.getId()
                )
        ).isEmpty();
    }

    @Test
    @DisplayName("재직 중인 MANAGER만 조회한다")
    void findOnlyActiveManagers() {
        Workplace workplace =
            createWorkplace("매장A", "TESTA004");

        User activeManager =
            createUser("manager4@test.com", "재직관리자");
        User employee =
            createUser("employee4@test.com", "일반직원");
        User retiredManager =
            createUser("retired4@test.com", "퇴사관리자");

        createMember(
            workplace, activeManager, WorkplaceRole.MANAGER, null
        );
        createMember(
            workplace, employee, WorkplaceRole.EMPLOYEE, null
        );
        createMember(
            workplace, retiredManager, WorkplaceRole.MANAGER, LEFT_AT
        );

        entityManager.flush();
        entityManager.clear();

        List<WorkplaceMember> result =
            workplaceMemberRepository
                .findAllByWorkplace_IdAndRoleAndLeftAtIsNull(
                    workplace.getId(),
                    WorkplaceRole.MANAGER
                );

        assertThat(result)
            .extracting(member -> member.getUser().getId())
            .containsExactly(activeManager.getId());
    }

    private User createUser(String email, String name) {
        return entityManager.persist(
            new User(email, "test-password", name)
        );
    }

    private Workplace createWorkplace(String name, String inviteCode) {
        return entityManager.persist(
            new Workplace(name, inviteCode)
        );
    }

    private WorkplaceMember createMember(
        Workplace workplace,
        User user,
        WorkplaceRole role,
        LocalDateTime leftAt
    ) {
        return entityManager.persist(
            new WorkplaceMember(
                workplace,
                user,
                role,
                JOINED_AT,
                leftAt
            )
        );
    }
}
