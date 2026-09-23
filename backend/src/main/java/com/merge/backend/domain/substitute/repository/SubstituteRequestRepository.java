package com.merge.backend.domain.substitute.repository;

import com.merge.backend.domain.substitute.entity.RequestStatus;
import com.merge.backend.domain.substitute.entity.SubstituteRequest;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubstituteRequestRepository extends JpaRepository<SubstituteRequest, Long>{

    boolean existsByShift_IdAndStatusIn(Long shiftId, Collection<RequestStatus> statuses);
    @EntityGraph(attributePaths = {"shift.schedule.workplace"})
    List<SubstituteRequest>findAllByRequesterMember_User_IdOrderByCreateDateDesc(Long userId);
}
