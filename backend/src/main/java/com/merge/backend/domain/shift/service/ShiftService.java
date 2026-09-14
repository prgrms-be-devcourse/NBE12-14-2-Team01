//package com.merge.backend.domain.shift.service;
//
//import com.merge.backend.domain.shift.entity.RegularShiftPattern;
//import com.merge.backend.domain.shift.entity.Shift;
//import java.time.DayOfWeek;
//import java.time.LocalDateTime;
//import org.springframework.stereotype.Service;
//
//@Service
//public class ShiftService {
//
//    public Shift create(RegularShiftPattern reqBody, int scheduleId) {
//        LocalDateTime startAt = parseDayOfWeek(String.valueOf(reqBody.getDayOfWeek()));
//    }
//
//    // 한글 요일을 DayOfWeek Enum으로 변환
//    private LocalDateTime parseDayOfWeek(String dayOfWeekStr) {
//        DayOfWeek targetDay = switch (dayOfWeekStr) {
//            case "월요일", "MONDAY" -> DayOfWeek.MONDAY;
//            case "화요일", "TUESDAY" -> DayOfWeek.TUESDAY;
//            case "수요일", "WEDNESDAY" -> DayOfWeek.WEDNESDAY;
//            case "목요일", "THURSDAY" -> DayOfWeek.THURSDAY;
//            case "금요일", "FRIDAY" -> DayOfWeek.FRIDAY;
//            case "토요일", "SATURDAY" -> DayOfWeek.SATURDAY;
//            case "일요일", "SUNDAY" -> DayOfWeek.SUNDAY;
//            default -> throw new IllegalArgumentException("유효하지 않은 요일입니다: " + dayOfWeekStr);
//        };
//
//        return
//    }
//}
