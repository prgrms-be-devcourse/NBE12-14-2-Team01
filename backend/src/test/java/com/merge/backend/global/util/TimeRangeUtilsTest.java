package com.merge.backend.global.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

public class TimeRangeUtilsTest {

    @Test
    void 시작_시각이_종료_시각보다_앞서면_유효한_범위이다() {
        // given
        LocalDateTime start =
            LocalDateTime.of(2026, 9, 16, 13, 59);

        LocalDateTime end =
            LocalDateTime.of(2026, 9, 16, 14, 0);

        // when
        boolean result =
            TimeRangeUtils.isValidRange(start, end);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void 시작_시각과_종료_시각이_같으면_유효하지_않다() {
        // given
        LocalDateTime start =
            LocalDateTime.of(2026, 9, 16, 14, 0);

        LocalDateTime end =
            LocalDateTime.of(2026, 9, 16, 14, 0);

        // when
        boolean result =
            TimeRangeUtils.isValidRange(start, end);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void 시작_시각이_종료_시각보다_늦으면_유효하지_않다() {
        // given
        LocalDateTime start =
            LocalDateTime.of(2026, 9, 16, 15, 0);

        LocalDateTime end =
            LocalDateTime.of(2026, 9, 16, 14, 0);

        // when
        boolean result =
            TimeRangeUtils.isValidRange(start, end);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void 다음_날까지_이어지는_시간_범위도_순서가_맞으면_유효하다() {
        // given
        LocalDateTime start =
            LocalDateTime.of(2026, 9, 16, 23, 0);

        LocalDateTime end =
            LocalDateTime.of(2026, 9, 17, 1, 0);

        // when
        boolean result =
            TimeRangeUtils.isValidRange(start, end);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void 두_시간_구간이_부분적으로_겹치면_true를_반환한다() {
        // given
        LocalDateTime aStart =
            LocalDateTime.of(2026, 9, 16, 10, 0);
        LocalDateTime aEnd =
            LocalDateTime.of(2026, 9, 16, 14, 0);

        LocalDateTime bStart =
            LocalDateTime.of(2026, 9, 16, 13, 0);
        LocalDateTime bEnd =
            LocalDateTime.of(2026, 9, 16, 18, 0);

        // when
        boolean result =
            TimeRangeUtils.overlaps(
                aStart,
                aEnd,
                bStart,
                bEnd
            );

        // then
        assertThat(result).isTrue();
    }

    @Test
    void 첫_구간의_끝과_두번째_구간의_시작이_같으면_겹치지_않는다() {
        // given
        LocalDateTime aStart =
            LocalDateTime.of(2026, 9, 16, 10, 0);
        LocalDateTime aEnd =
            LocalDateTime.of(2026, 9, 16, 14, 0);

        LocalDateTime bStart =
            LocalDateTime.of(2026, 9, 16, 14, 0);
        LocalDateTime bEnd =
            LocalDateTime.of(2026, 9, 16, 18, 0);

        // when
        boolean result =
            TimeRangeUtils.overlaps(
                aStart,
                aEnd,
                bStart,
                bEnd
            );

        // then
        assertThat(result).isFalse();
    }

    @Test
    void 두_시간_구간이_완전히_떨어져_있으면_false를_반환한다() {
        // given
        LocalDateTime aStart =
            LocalDateTime.of(2026, 9, 16, 10, 0);
        LocalDateTime aEnd =
            LocalDateTime.of(2026, 9, 16, 14, 0);

        LocalDateTime bStart =
            LocalDateTime.of(2026, 9, 16, 15, 0);
        LocalDateTime bEnd =
            LocalDateTime.of(2026, 9, 16, 18, 0);

        // when
        boolean result =
            TimeRangeUtils.overlaps(
                aStart,
                aEnd,
                bStart,
                bEnd
            );

        // then
        assertThat(result).isFalse();
    }

    @Test
    void 한_시간_구간이_다른_구간을_포함하면_true를_반환한다() {
        // given
        LocalDateTime aStart =
            LocalDateTime.of(2026, 9, 16, 10, 0);
        LocalDateTime aEnd =
            LocalDateTime.of(2026, 9, 16, 18, 0);

        LocalDateTime bStart =
            LocalDateTime.of(2026, 9, 16, 12, 0);
        LocalDateTime bEnd =
            LocalDateTime.of(2026, 9, 16, 14, 0);

        // when
        boolean result =
            TimeRangeUtils.overlaps(
                aStart,
                aEnd,
                bStart,
                bEnd
            );

        // then
        assertThat(result).isTrue();
    }

    @Test
    void 두_시간_구간이_완전히_같으면_true를_반환한다() {
        // given
        LocalDateTime aStart =
            LocalDateTime.of(2026, 9, 16, 10, 0);
        LocalDateTime aEnd =
            LocalDateTime.of(2026, 9, 16, 14, 0);

        LocalDateTime bStart =
            LocalDateTime.of(2026, 9, 16, 10, 0);
        LocalDateTime bEnd =
            LocalDateTime.of(2026, 9, 16, 14, 0);

        // when
        boolean result =
            TimeRangeUtils.overlaps(
                aStart,
                aEnd,
                bStart,
                bEnd
            );

        // then
        assertThat(result).isTrue();
    }

}
