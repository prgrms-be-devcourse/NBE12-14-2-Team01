package com.merge.backend.domain.workplace.repository;

import com.merge.backend.domain.workplace.entity.Workplace;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkplaceRepository extends JpaRepository<Workplace, Long> {

    boolean existsByInviteCode(String inviteCode);

    Optional<Workplace> findByInviteCode(String inviteCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT w
        FROM Workplace w
        WHERE w.id = :workplaceId
        """)
    Optional<Workplace> findByIdForUpdate(
        @Param("workplaceId") Long workplaceId
    );
}
