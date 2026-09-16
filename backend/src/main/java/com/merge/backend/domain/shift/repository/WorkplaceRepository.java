package com.merge.backend.domain.shift.repository;

import com.merge.backend.domain.workplace.entity.Workplace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkplaceRepository extends JpaRepository<Workplace, Long> {
}
