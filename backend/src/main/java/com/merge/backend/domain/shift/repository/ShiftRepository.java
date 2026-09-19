package com.merge.backend.domain.shift.repository;

import com.merge.backend.domain.shift.entity.ScheduleStatus;
import com.merge.backend.domain.shift.entity.Shift;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShiftRepository extends JpaRepository<Shift, Long> {

    List<Shift> findBySchedule_IdOrderByStartAtAscIdAsc(
        Long scheduleId
    );

    //동일 Schedule 내 동일 WorkplaceMember의 다른 Shift 중복 검증
    @Query("""
        SELECT COUNT(s) > 0 
        FROM Shift s 
        WHERE s.schedule.id = :scheduleId 
          AND s.member.id = :memberId 
          AND s.startAt < :endAt 
          AND s.endAt > :startAt
          AND (:currentShiftId IS NULL OR s.id != :currentShiftId)
          AND s.status = ShiftStatus.SCHEDULED
        """)
    boolean existsOverlappingInSchedule(
        @Param("scheduleId") Long scheduleId,
        @Param("memberId") Long memberId,
        @Param("startAt") LocalDateTime startAt,
        @Param("endAt") LocalDateTime endAt,
        @Param("currentShiftId") Long currentShiftId
    );

    //해당 User의 모든 Workplace "공식/확정 Shift" 중복 검증 (투잡/타매장 포함)
    @Query("""
        SELECT COUNT(s) > 0 
        FROM Shift s 
        WHERE s.member.user.id = :userId 
          AND s.schedule.status = :scheduleStatus
          AND s.startAt < :endAt 
          AND s.endAt > :startAt
          AND (:currentShiftId IS NULL OR s.id != :currentShiftId)
          AND s.status = ShiftStatus.SCHEDULED
        """)
    boolean existsOverlappingOfficialShift(
        @Param("userId") Long userId,
        @Param("scheduleStatus") ScheduleStatus scheduleStatus,
        @Param("startAt") LocalDateTime startAt,
        @Param("endAt") LocalDateTime endAt,
        @Param("currentShiftId") Long currentShiftId
    );

    @Query("""
        SELECT s
        FROM Shift s 
        WHERE s.member.user.id = :currentUserId 
          AND s.member.leftAt IS NULL
          AND s.schedule.status = ScheduleStatus.PUBLISHED
          AND s.status = ShiftStatus.SCHEDULED
          AND s.schedule.weekStartDate = :weekStartDate
        ORDER BY s.startAt ASC
        """)
    List<Shift> findAllByWeekStartDateAndCurrentUserId(
        @Param("weekStartDate") LocalDate weekStartDate,
        @Param("currentUserId") Long currentUserId
    );
}
