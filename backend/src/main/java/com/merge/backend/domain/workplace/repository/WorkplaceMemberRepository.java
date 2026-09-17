package com.merge.backend.domain.workplace.repository;

import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkplaceMemberRepository extends JpaRepository<WorkplaceMember, Long> {

    Optional<WorkplaceMember> findByWorkplaceIdAndUserId(Long workplaceId, Long actorId);
}
