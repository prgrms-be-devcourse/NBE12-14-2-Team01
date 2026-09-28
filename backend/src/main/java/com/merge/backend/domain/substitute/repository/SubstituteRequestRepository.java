package com.merge.backend.domain.substitute.repository;

import com.merge.backend.domain.substitute.entity.RequestStatus;
import com.merge.backend.domain.substitute.entity.SubstituteRequest;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubstituteRequestRepository extends JpaRepository<SubstituteRequest, Long>{

    boolean existsByShift_IdAndStatusIn(Long shiftId, Collection<RequestStatus> statuses);
    @EntityGraph(attributePaths = {"shift.schedule.workplace"})
    List<SubstituteRequest>findAllByRequesterMember_User_IdOrderByCreateDateDesc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r
        FROM SubstituteRequest r
        WHERE r.id = :requestId
        """)
    Optional<SubstituteRequest> findByIdForUpdate(
        @Param("requestId") Long requestId
    );

    //해당 근무지의 모든 대체근무 요청 조회
    //근무 시간 순으로 정렬하되, 시간이 같으면 id 기준으로 정렬
    @Query("""
        SELECT s
        FROM SubstituteRequest s 
        JOIN FETCH s.shift sh
        JOIN FETCH s.requesterMember rm
        JOIN FETCH rm.user
        JOIN FETCH rm.workplace
        WHERE s.shift.schedule.workplace.id = :workplaceId
          AND s.status IN (com.merge.backend.domain.substitute.entity.RequestStatus.OPEN,
                            com.merge.backend.domain.substitute.entity.RequestStatus.ACCEPTED)
          AND s.shift.schedule.status = 
                  com.merge.backend.domain.shift.entity.ScheduleStatus.PUBLISHED
          AND s.shift.status = com.merge.backend.domain.shift.entity.ShiftStatus.SCHEDULED
          AND s.shift.startAt > :now
        ORDER BY s.shift.startAt ASC, s.id ASC 
        """)
    List<SubstituteRequest> findAllByWorkplaceId(
        @Param("workplaceId") Long workplaceId,
        @Param("now") LocalDateTime now
    );
}
