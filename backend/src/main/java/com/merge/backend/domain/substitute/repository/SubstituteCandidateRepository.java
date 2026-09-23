package com.merge.backend.domain.substitute.repository;

import com.merge.backend.domain.substitute.entity.CandidateStatus;
import com.merge.backend.domain.substitute.entity.SubstituteCandidate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubstituteCandidateRepository extends JpaRepository<SubstituteCandidate, Long> {
    // 특정 Request에 대해 상태가 ACCEPTED인 Candidate 단건 조회
    Optional<SubstituteCandidate> findByRequestIdAndStatus(Long requestId, CandidateStatus status);

    //후보자가 다른 대체 요청에서 이미 수락한 활성 대타 약속 중, 동시간대 겹침이 있는지 확인
    @Query("""
        select count(c) > 0 
        from SubstituteCandidate c 
        where c.member.user.id = :userId 
            and c.request.id <> :requestId
            and c.status = CandidateStatus.ACCEPTED
            and c.request.status = RequestStatus.ACCEPTED 
            and c.request.shift.startAt < :endAt 
            and c.request.shift.endAt > :startAt
            and c.request.shift.startAt > :now
        """)
    boolean existsConflictingActiveSubstitute(
        @Param("userId") Long userId,
        @Param("requestId") Long requestId,
        @Param("startAt") LocalDateTime startAt,
        @Param("endAt") LocalDateTime endAt,
        @Param("now") LocalDateTime now
    );

    // N+1 해결용 - IN 절로 한 번에 조회
    @Query("""
        SELECT c
        FROM SubstituteCandidate c
        JOIN FETCH c.member m
        JOIN FETCH m.user
        WHERE c.request.id IN :requestIds
          AND c.status = :status
        """)
    List<SubstituteCandidate> findByRequestIdInAndStatus(
        @Param("requestIds") List<Long> requestIds,
        @Param("status") CandidateStatus status
    );
}
