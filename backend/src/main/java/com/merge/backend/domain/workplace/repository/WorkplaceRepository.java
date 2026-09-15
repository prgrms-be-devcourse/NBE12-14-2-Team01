package com.merge.backend.domain.workplace.repository;

import com.merge.backend.domain.workplace.entity.Workplace;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkplaceRepository extends JpaRepository<Workplace, Long> {

    boolean existsByInviteCode(String inviteCode);

    Optional<Workplace> findAllByInviteCode(String inviteCode);

}
