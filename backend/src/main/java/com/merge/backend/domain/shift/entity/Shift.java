package com.merge.backend.domain.shift.entity;

import com.merge.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Getter
@NoArgsConstructor
public class Shift extends BaseEntity {

    @Column(nullable = false)
    int scheduleId;

    @Column(nullable = false)
    int memberId;

    LocalDateTime startAt;

    LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;


    public enum Status {
        BEFORE_WORK,
        DURING_WORK,
        AFTER_WORK
    };
}
