package com.merge.backend.domain.shift.repository;

import com.merge.backend.domain.shift.entity.RegularShiftPattern;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface RegularShiftPatternRepository
        extends JpaRepository<RegularShiftPattern, Long> {

    List<RegularShiftPattern> findByMemberIdAndDayOfWeek(Long memberId, DayOfWeek dayOfWeek);
    List<RegularShiftPattern> findByMemberWorkplaceIdAndMemberLeftAtIsNull(
            Long workplaceId
    );
}
