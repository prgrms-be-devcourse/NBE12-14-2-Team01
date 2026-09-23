package com.merge.backend.domain.substitute.entity;

import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"request_id", "member_id"})
    }
)
public class SubstituteCandidate extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private SubstituteRequest request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private WorkplaceMember member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CandidateStatus status;

    private LocalDateTime respondedAt;

    public SubstituteCandidate(
        SubstituteRequest request,
        WorkplaceMember member
    ) {
        this.request = request;
        this.member = member;
        this.status = CandidateStatus.PENDING;
    }

    public void accept(LocalDateTime now) {
        this.status = CandidateStatus.ACCEPTED; // 대타 요청을 수락한 상태
        this.respondedAt = now;                 // 수락한 시간
    }

    public void reject(LocalDateTime now) {
        this.status = CandidateStatus.REJECTED; // 대타 요청을 거절한 상태
        this.respondedAt = now;                 // 거절한 시간
    }
}