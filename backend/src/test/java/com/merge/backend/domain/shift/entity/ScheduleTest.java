package com.merge.backend.domain.shift.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.merge.backend.domain.workplace.entity.Workplace;
import java.time.DayOfWeek;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ScheduleTest {

    @Test
    void 새로운_스케줄은_DRAFT_상태로_생성된다() {
        Workplace workplace =
            mock(Workplace.class);

        LocalDate weekStartDate =
            LocalDate.of(2026, 9, 21);

        Schedule schedule =
            new Schedule(
                workplace,
                weekStartDate
            );

        assertThat(schedule.getWorkplace())
            .isSameAs(workplace);

        assertThat(schedule.getWeekStartDate())
            .isEqualTo(weekStartDate);

        assertThat(schedule.getStatus())
            .isEqualTo(ScheduleStatus.DRAFT);

        assertThat(schedule.getPublishedAt())
            .isNull();
    }

    @Test
    void 월요일은_주_시작일과_같다() {
        // given
        Workplace workplace =
            mock(Workplace.class);

        Schedule schedule =
            new Schedule(
                workplace,
                LocalDate.of(2026, 9, 21)
            );

        // when
        LocalDate result =
            schedule.resolveDate(
                DayOfWeek.MONDAY
            );

        // then
        assertThat(result)
            .isEqualTo(
                LocalDate.of(2026, 9, 21)
            );
    }

    @Test
    void 수요일은_주_시작일로부터_이틀_뒤이다() {
        // given
        Workplace workplace =
            mock(Workplace.class);

        Schedule schedule =
            new Schedule(
                workplace,
                LocalDate.of(2026, 9, 21)
            );

        // when
        LocalDate result =
            schedule.resolveDate(
                DayOfWeek.WEDNESDAY
            );

        // then
        assertThat(result)
            .isEqualTo(
                LocalDate.of(2026, 9, 23)
            );
    }

    @Test
    void 일요일은_주_시작일로부터_육일_뒤이다() {
        // given
        Workplace workplace =
            mock(Workplace.class);

        Schedule schedule =
            new Schedule(
                workplace,
                LocalDate.of(2026, 9, 21)
            );

        // when
        LocalDate result =
            schedule.resolveDate(
                DayOfWeek.SUNDAY
            );

        // then
        assertThat(result)
            .isEqualTo(
                LocalDate.of(2026, 9, 27)
            );
    }

}