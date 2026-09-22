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
        WorkplaceMember member,
        CandidateStatus status
    ) {
        this.request = request;
        this.member = member;
        this.status = status;
    }
    //1. 후보자 중 1명이 수락을 눌렀을 때
    public void acceptRequest(LocalDateTime now) {

        //본인 상태 업데이트
        this.status = CandidateStatus.ACCEPTED;
        this.respondedAt = now;

        //연관된 대체근무 요청 상태 승인으로 변경
        this.request.accept();
    }
}