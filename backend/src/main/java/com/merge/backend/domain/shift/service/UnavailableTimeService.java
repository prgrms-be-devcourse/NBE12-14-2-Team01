package com.merge.backend.domain.shift.service;

import com.merge.backend.domain.shift.entity.ScheduleStatus;
import com.merge.backend.domain.shift.entity.UnavailableTime;
import com.merge.backend.domain.shift.exception.UnavailableTimeErrorCode;
import com.merge.backend.domain.shift.repository.ShiftRepository;
import com.merge.backend.domain.shift.repository.UnavailableTimeRepository;
import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.user.service.UserService;
import com.merge.backend.global.dto.ConfirmationRequiredResponse;
import com.merge.backend.global.exception.BusinessException;
import com.merge.backend.global.rq.Rq;
import com.merge.backend.global.util.TimeRangeUtils;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UnavailableTimeService {

    private final UnavailableTimeRepository unavailableTimeRepository;
    private final ShiftRepository shiftRepository;
    private final Rq rq;
    private final Clock clock;
    private final UserService userService;

    public record UnavailableTimeResult(
            Long unavailableTimeId,
            LocalDateTime startAt,
            LocalDateTime endAt,
            boolean officialShiftConflict
    ){}

    @Transactional(readOnly = true)
    public List<UnavailableTimeResult> findAll() {

        Long userId = rq.getActorId();

        LocalDateTime now =
            LocalDateTime.now(clock);

        List<UnavailableTime> unavailableTimes =
            unavailableTimeRepository
                .findByUserIdAndEndAtAfterOrderByStartAtAsc(
                    userId,
                    now
                );

        if (unavailableTimes.isEmpty()) {
            return List.of();
        }

        Set<Long> officialShiftConflictIds =
            new HashSet<>(
                unavailableTimeRepository
                    .findIdsWithOfficialShiftConflict(
                        userId,
                        now
                    )
            );

        return unavailableTimes.stream()
            .map(unavailableTime ->
                new UnavailableTimeResult(
                    unavailableTime.getId(),
                    unavailableTime.getStartAt(),
                    unavailableTime.getEndAt(),
                    officialShiftConflictIds.contains(
                        unavailableTime.getId()
                    )
                )
            )
            .toList();
    }

    public UnavailableTime register(LocalDateTime startAt,
                                    LocalDateTime endAt,
                                    Boolean confirmOfficialShiftConflict) {
        Long userId = rq.getActorId();

        User user =
            userService.getByIdForUpdate(userId);

        LocalDateTime now =
            LocalDateTime.now(clock);

        validateTimeRange(startAt, endAt);
        validateFutureTime(startAt, now);
        validateUnavailableTimeOverlap(userId,
                null,
                startAt,
                endAt);
        validateOfficialShiftConflict(userId,
                startAt,
                endAt,
                confirmOfficialShiftConflict);

        UnavailableTime newUnavailableTime = new UnavailableTime(
                user,
                startAt,
                endAt
        );

        return unavailableTimeRepository.save(newUnavailableTime);

    }

    public UnavailableTime update(Long unavailableTimeId,
                                  LocalDateTime startAt,
                                  LocalDateTime endAt,
                                  Boolean confirmOfficialShiftConflict) {
        Long userId =
            rq.getActorId();

        userService.getByIdForUpdate(
            userId
        );

        LocalDateTime now =
            LocalDateTime.now(clock);

        UnavailableTime unavailableTime =
            unavailableTimeRepository.findById(unavailableTimeId)
                .orElseThrow( () ->
                    new BusinessException(
                        UnavailableTimeErrorCode.NOT_FOUND
                )
        );

        validateOwner(userId, unavailableTime);
        validateExistingFutureTime(unavailableTime, now);
        validateTimeRange(
                startAt,
                endAt
        );
        validateFutureTime(startAt, now);
        validateUnavailableTimeOverlap(
                userId,
                unavailableTimeId,
                startAt,
                endAt
        );
        validateOfficialShiftConflict(
                userId,
                startAt,
                endAt,
                confirmOfficialShiftConflict
        );

        unavailableTime.update(
                startAt,
                endAt
        );

        return unavailableTime;
        
    }

    public void delete(Long unavailableTimeId) {

        Long userId =
            rq.getActorId();

        userService.getByIdForUpdate(
            userId
        );

        UnavailableTime unavailableTime =
                unavailableTimeRepository.findById(unavailableTimeId).orElseThrow(
                        () -> new BusinessException(
                                UnavailableTimeErrorCode.NOT_FOUND
                        )
                );

        validateOwner(userId, unavailableTime);

        unavailableTimeRepository.delete(unavailableTime);
    }


    //본인 일정인가
    private void validateOwner(
            Long userId,
            UnavailableTime unavailableTime
    ) {
        if(!unavailableTime.getUser().getId().equals(userId)){
            throw new BusinessException(
                    UnavailableTimeErrorCode.FORBIDDEN
            );
        }
    }

    //새로운 시작 시간이 미래인지
    private void validateFutureTime(
            LocalDateTime startAt,
            LocalDateTime now
    ) {
        if(!startAt.isAfter(now)) {
            throw new BusinessException(
                    UnavailableTimeErrorCode.NOT_FUTURE_TIME
            );
        }
    }

    //기존 일정 자체가 아직 시작 전인지
    private void validateExistingFutureTime(
            UnavailableTime unavailableTime,
            LocalDateTime now
    ) {
        if(!unavailableTime.getStartAt()
                .isAfter(now)) {
            throw new BusinessException(
                    UnavailableTimeErrorCode.NOT_MODIFIABLE_TIME
            );
        }
    }

    //시작시간<끝나는 시간
    private void validateTimeRange(
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        if(!TimeRangeUtils.isValidRange(startAt, endAt)) {
            throw new BusinessException(
                    UnavailableTimeErrorCode.INVALID_TIME_RANGE
            );
        }
    }

    //본인 불가능 일정이랑 겹치는가
    private void validateUnavailableTimeOverlap(
        Long userId,
        Long excludeUnavailableTimeId,
        LocalDateTime startAt,
        LocalDateTime endAt
    ) {

        boolean overlaps;

        if (excludeUnavailableTimeId == null) {

            overlaps =
                unavailableTimeRepository
                    .existsOverlappingUnavailableTime(
                        userId,
                        startAt,
                        endAt
                    );

        } else {

            overlaps =
                unavailableTimeRepository
                    .existsOverlappingUnavailableTimeExcludingId(
                        userId,
                        excludeUnavailableTimeId,
                        startAt,
                        endAt
                    );
        }

        if (overlaps) {
            throw new BusinessException(
                UnavailableTimeErrorCode.TIME_OVERLAP
            );
        }
    }

    //shift하고 시간이 겹치는가
    private void validateOfficialShiftConflict(
            Long userId,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Boolean confirmOfficialShiftConflict
    ) {
        boolean hasConflictShift =
                shiftRepository.existsOverlappingOfficialShift(
                        userId,
                        ScheduleStatus.PUBLISHED,
                        startAt,
                        endAt,
                        null
                );

        if(hasConflictShift && !Boolean.TRUE.equals(
                confirmOfficialShiftConflict
        )) {
            throw new BusinessException(
                    UnavailableTimeErrorCode.OFFICIAL_SHIFT_CONFLICT,
                    new ConfirmationRequiredResponse(true)
            );
        }
    }


}
