package com.merge.backend.domain.shift.repository;

import com.merge.backend.domain.shift.entity.Schedule;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    boolean existsByWorkplace_IdAndWeekStartDate(
        Long workplaceId,
        LocalDate weekStartDate
    );

}