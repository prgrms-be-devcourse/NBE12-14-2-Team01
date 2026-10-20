
package com.merge.backend.domain.workplace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.workplace.entity.Workplace;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.domain.workplace.entity.WorkplaceRole;
import com.merge.backend.domain.workplace.exception.WorkplaceErrorCode;
import com.merge.backend.domain.workplace.repository.WorkplaceMemberRepository;
import com.merge.backend.domain.workplace.repository.WorkplaceRepository;
import com.merge.backend.global.config.TimeConfig;
import com.merge.backend.global.exception.BusinessException;
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
}
