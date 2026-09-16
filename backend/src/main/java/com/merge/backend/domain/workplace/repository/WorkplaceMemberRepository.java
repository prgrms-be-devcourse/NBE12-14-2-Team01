package com.merge.backend.domain.workplace.repository;

import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.workplace.entity.Workplace;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkplaceMemberRepository extends JpaRepository<WorkplaceMember, Long> {

    boolean existsByWorkplaceAndUser(Workplace workplace, User user);

    Optional<WorkplaceMember> findByWorkplace_IdAndUser_IdAndLeftAtIsNull(
        Long workplaceId,
        Long userId
    );

    List<WorkplaceMember> findAllByUser_IdAndLeftAtIsNull(Long userId);
}
