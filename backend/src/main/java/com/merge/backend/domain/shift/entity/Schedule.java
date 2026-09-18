package com.merge.backend.domain.shift.entity;

import com.merge.backend.domain.workplace.entity.Workplace;
import com.merge.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"workplace_id", "week_start_date"})
    }
)
public class Schedule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workplace_id", nullable = false)
    private Workplace workplace;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleStatus status;

    private LocalDateTime publishedAt;

    public Schedule(
        Workplace workplace,
        LocalDate weekStartDate
    ) {
        this.workplace = workplace;
        this.weekStartDate = weekStartDate;
        this.status = ScheduleStatus.DRAFT;
        this.publishedAt = null;
    }

    public void publish(
        LocalDateTime publishedAt
    ) {
        this.status = ScheduleStatus.PUBLISHED;
        this.publishedAt = publishedAt;
    }

    public LocalDate resolveDate(
        DayOfWeek dayOfWeek
    ) {
        long daysToAdd =
            dayOfWeek.getValue()
                - DayOfWeek.MONDAY.getValue();

        return weekStartDate.plusDays(daysToAdd);
    }

}