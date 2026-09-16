package com.merge.backend.global.util;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TimeRangeUtils {

    // 시간 구간 자체의 유효성 검사
    public static boolean isValidRange(
        LocalDateTime start,
        LocalDateTime end
    ) {
        return start.isBefore(end);
    }

    // 유효한 두 시간 구간의 겹침 계산
    public static boolean overlaps(
        LocalDateTime aStart,
        LocalDateTime aEnd,
        LocalDateTime bStart,
        LocalDateTime bEnd
    ) {
        return aStart.isBefore(bEnd)
            && bStart.isBefore(aEnd);
    }

}