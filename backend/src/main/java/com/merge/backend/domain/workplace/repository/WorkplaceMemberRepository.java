package com.merge.backend.domain.workplace.repository;

import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkplaceMemberRepository extends JpaRepository<WorkplaceMember, Long> {
    Optional<WorkplaceMember> findByWorkplaceIdAndUserId(
            Long workplaceId,
            Long userId
    );

}
