package com.merge.backend.global.entity;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.merge.backend.global.config.TestClock;
import com.merge.backend.global.config.TestTimeConfig;
import com.merge.backend.global.config.TimeConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({
    TimeConfig.class,
    TestTimeConfig.class
})
class BaseEntityAuditingTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TestClock testClock;

    @BeforeEach
    void setUp() {
        testClock.setInstant(
            Instant.parse("2026-09-15T01:00:00Z")
        );
    }

    @Test
    void 저장_시_Clock_기준으로_생성일과_수정일이_기록된다() {
        // given
        AuditingTestEntity entity = new AuditingTestEntity("before");

        // when
        entityManager.persist(entity);
        entityManager.flush();

        // then
        LocalDateTime expected = LocalDateTime.of(
            2026, 9, 15, 10, 0
        );

        assertThat(entity.getCreateDate()).isEqualTo(expected);
        assertThat(entity.getModifyDate()).isEqualTo(expected);
    }

    @Test
    void 수정_시_생성일은_유지되고_수정일만_Clock_기준으로_갱신된다() {
        // given
        AuditingTestEntity entity = new AuditingTestEntity("before");

        entityManager.persist(entity);
        entityManager.flush();

        Long entityId = entity.getId();
        LocalDateTime createdAt = entity.getCreateDate();

        testClock.setInstant(
            Instant.parse("2026-09-15T02:00:00Z")
        );

        // when
        entity.changeName("after");
        entityManager.flush();
        entityManager.clear();

        // DB에서 다시 조회
        AuditingTestEntity found =
            entityManager.find(AuditingTestEntity.class, entityId);

        // then
        LocalDateTime expectedModifiedAt =
            LocalDateTime.of(2026, 9, 15, 11, 0);

        assertThat(found.getCreateDate()).isEqualTo(createdAt);
        assertThat(found.getModifyDate()).isEqualTo(expectedModifiedAt);
    }

}