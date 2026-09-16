package com.merge.backend.domain.shift.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.merge.backend.domain.shift.exception.ScheduleErrorCode;
import com.merge.backend.domain.shift.repository.ScheduleRepository;
import com.merge.backend.global.exception.BusinessException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @InjectMocks
    private ScheduleService scheduleService;

    @Test
    void 주_시작일이_월요일이_아니면_예외가_발생한다() {
        // given
        Long actorUserId = 1L;
        Long workplaceId = 1L;
        LocalDate weekStartDate =
            LocalDate.of(2026, 9, 22); // 화요일

        // when
        BusinessException exception =
            assertThrows(
                BusinessException.class,
                () -> scheduleService.createDraftSchedule(
                    actorUserId,
                    workplaceId,
                    weekStartDate
                )
            );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(
                ScheduleErrorCode.INVALID_WEEK_START_DATE
            );

        verifyNoInteractions(scheduleRepository);
    }

    @Test
    void 같은_주차의_스케줄이_이미_존재하면_예외가_발생한다() {
        // given
        Long actorUserId = 1L;
        Long workplaceId = 1L;
        LocalDate weekStartDate =
            LocalDate.of(2026, 9, 21); // 월요일

        when(
            scheduleRepository
                .existsByWorkplace_IdAndWeekStartDate(
                    workplaceId,
                    weekStartDate
                )
        ).thenReturn(true);

        // when
        BusinessException exception =
            assertThrows(
                BusinessException.class,
                () -> scheduleService.createDraftSchedule(
                    actorUserId,
                    workplaceId,
                    weekStartDate
                )
            );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(
                ScheduleErrorCode.SCHEDULE_ALREADY_EXISTS
            );

        verify(scheduleRepository)
            .existsByWorkplace_IdAndWeekStartDate(
                workplaceId,
                weekStartDate
            );

    }

}