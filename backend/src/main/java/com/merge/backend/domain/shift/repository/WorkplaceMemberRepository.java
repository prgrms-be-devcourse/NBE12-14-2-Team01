package com.merge.backend.domain.shift.repository;

import com.merge.backend.domain.workplace.entity.Workplace;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkplaceMemberRepository
        extends JpaRepository<WorkplaceMember, Long> {
}
