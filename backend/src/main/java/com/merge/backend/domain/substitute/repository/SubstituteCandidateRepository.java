package com.merge.backend.domain.substitute.repository;

import com.merge.backend.domain.substitute.entity.SubstituteCandidate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
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

    @EntityGraph(attributePaths = {
        "request",                          // 대타 요청 자체
        "request.shift",                    // 대타가 필요한 근무
        "request.shift.schedule",           // 그 근무가 속한 근무표
        "request.shift.schedule.workplace", // 어느 근무지인지
        "request.requesterMember",          // 대타를 요청한 멤버
        "request.requesterMember.user"      // 요청한 사람의 이름 등 User 정보
    })
    @Query("""
    SELECT c
    FROM SubstituteCandidate c
    WHERE c.member.user.id = :userId
      AND c.member.leftAt IS NULL
      AND c.status = 'PENDING'
      AND c.request.status = 'OPEN'
      AND c.request.shift.startAt > :now
      AND c.request.shift.schedule.status = 'PUBLISHED'
      AND c.request.shift.status = 'SCHEDULED'
    """)
    List<SubstituteCandidate> findReceivedRequests(
        @Param("userId") Long userId,   // 현재 로그인한 사용자
        @Param("now") LocalDateTime now // 현재 시간
    );
}
