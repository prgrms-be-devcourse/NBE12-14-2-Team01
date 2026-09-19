package com.merge.backend.domain.shift.dto;

import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.entity.ShiftStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ShiftListResponse(
    LocalDate weekStartDate,
    List<ShiftItem> shifts
) {
    public static ShiftListResponse of(LocalDate weekStartDate, List<Shift> shifts) {
        List<ShiftItem> shiftItems = shifts.stream()
            .map(ShiftItem::from)
            .toList();

        return new ShiftListResponse(weekStartDate, shiftItems);
    }

    public record ShiftItem(
        Long shiftId,
        Long scheduleId,
        Long workplaceId,
        String workplaceName,
        LocalDateTime startAt,
        LocalDateTime endAt,
        ShiftStatus status
    ) {
        public static ShiftItem from(Shift shift) {
            return new ShiftItem(
                shift.getId(),
                shift.getSchedule().getId(),
                shift.getMember().getWorkplace().getId(),
                shift.getMember().getWorkplace().getName(),
                shift.getStartAt(),
                shift.getEndAt(),
                shift.getStatus()
            );
        }
    }
}
