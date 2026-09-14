package com.merge.backend.domain.shift.repository;

import com.merge.backend.domain.shift.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftRepository extends JpaRepository<Shift, Long> {

}
