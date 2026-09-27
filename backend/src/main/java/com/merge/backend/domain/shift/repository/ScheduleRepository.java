package com.merge.backend.domain.shift.repository;

import com.merge.backend.domain.shift.entity.Schedule;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    Optional<Schedule> findByWorkplace_IdAndWeekStartDate(
        Long workplaceId,
        LocalDate weekStartDate
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM Schedule s
        WHERE s.workplace.id = :workplaceId
          AND s.weekStartDate = :weekStartDate
        """)
    Optional<Schedule> findByWorkplaceAndWeekStartDateForUpdate(
        @Param("workplaceId") Long workplaceId,
        @Param("weekStartDate") LocalDate weekStartDate
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM Schedule s
        WHERE s.id = :scheduleId
        """)
    Optional<Schedule> findByIdForUpdate(
        @Param("scheduleId") Long scheduleId
    );

}