package com.merge.backend.domain.shift.repository;

import com.merge.backend.domain.shift.entity.UnavailableTime;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UnavailableTimeRepository extends JpaRepository<UnavailableTime, Long> {

    @Query("""
    SELECT COUNT(u) > 0 
    FROM UnavailableTime u 
    WHERE u.user.id = :userId 
      AND u.startAt < :endAt 
      AND u.endAt > :startAt
""")
    boolean existsOverlappingUnavailableTime(
        @Param("userId") Long userId,
        @Param("startAt") LocalDateTime startAt,
        @Param("endAt") LocalDateTime endAt
    );
}
