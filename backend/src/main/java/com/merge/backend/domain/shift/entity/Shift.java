package com.merge.backend.domain.shift.entity;

import com.merge.backend.domain.workplace.entity.WorkplaceMember;
import com.merge.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Shift extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private WorkplaceMember member;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShiftStatus status;

    public Shift (
        Schedule schedule,
        WorkplaceMember member,
        LocalDateTime startAt,
        LocalDateTime endAt,
        ShiftStatus status
    ) {
        this.schedule = schedule;
        this.member = member;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status;
    }

    public Shift update(
        WorkplaceMember member,
        LocalDateTime startAt,
        LocalDateTime endAt
    ) {
        this.member = member;
        this.startAt = startAt;
        this.endAt = endAt;

        return this;
    }
    //SubstituteRequest에서 넘어옴
    public void changeMember(WorkplaceMember member) {
        this.member = member;
    }
}