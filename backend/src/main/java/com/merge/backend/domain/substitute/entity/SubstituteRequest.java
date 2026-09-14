package com.merge.backend.domain.substitute.entity;

import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class SubstituteRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_member_id", nullable = false)
    private WorkplaceMember requesterMember;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    private LocalDateTime approvedAt;

    private LocalDateTime closedAt;
}