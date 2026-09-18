package com.merge.backend.domain.shift.entity;

import com.merge.backend.domain.shift.repository.UnavailableTimeRepository;
import com.merge.backend.domain.user.entity.User;
import com.merge.backend.global.entity.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class UnavailableTime extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    public UnavailableTime(
            User user,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        this.user = user;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public void update(
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        this.startAt = startAt;
        this.endAt = endAt;
    }
}