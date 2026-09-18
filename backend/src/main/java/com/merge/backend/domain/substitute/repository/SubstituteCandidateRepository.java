package com.merge.backend.domain.substitute.repository;

import com.merge.backend.domain.substitute.entity.SubstituteCandidate;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubstituteCandidateRepository extends JpaRepository<SubstituteCandidate, Long> {
    @Query(""" 
          SELECT COUNT(c) > 0
          FROM SubstituteCandidate c
          WHERE c.member.user.id = :userId
              AND c.status = 'ACCEPTED'
              AND c.request.status = 'ACCEPTED'
              AND c.request.shift.startAt < :endAt
              AND c.request.shift.endAt > :startAt          
          """)
    boolean existsOverlappingAcceptedSubstitute(
        @Param("userId") Long userId,
        @Param("startAt") LocalDateTime startAt,
        @Param("endAt") LocalDateTime endAt
    );
}
