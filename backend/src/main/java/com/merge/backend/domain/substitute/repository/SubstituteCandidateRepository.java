package com.merge.backend.domain.substitute.repository;

import com.merge.backend.domain.substitute.entity.CandidateStatus;
import com.merge.backend.domain.substitute.entity.SubstituteCandidate;
import java.time.LocalDateTime;
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
        where c.member.id = :memberId 
            and c.request.id <> :requestId
            and c.status = CandidateStatus.ACCEPTED
            and c.request.status = RequestStatus.ACCEPTED 
            and c.request.shift.startAt < :endAt 
            and c.request.shift.endAt > :startAt
            and c.request.shift.startAt > :now
        """)
    boolean existsConflictingActiveSubstitute(
        @Param("memberId") Long memberId,
        @Param("requestId") Long requestId,
        @Param("startAt") LocalDateTime startAt,
        @Param("endAt") LocalDateTime endAt,
        @Param("now") LocalDateTime now
    );
}
