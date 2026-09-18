package com.merge.backend.domain.substitute;

import com.merge.backend.domain.substitute.entity.RequestStatus;
import com.merge.backend.domain.substitute.entity.SubstituteRequest;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubstituteRequestRepository extends JpaRepository<SubstituteRequest, Long>{

    boolean existsByShift_idAndStatusIn(Long shiftId, Collection<RequestStatus> statuses);
}
