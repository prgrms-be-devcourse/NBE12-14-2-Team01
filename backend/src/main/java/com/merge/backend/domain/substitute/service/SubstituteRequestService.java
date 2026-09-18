package com.merge.backend.domain.substitute.service;

import com.merge.backend.domain.shift.entity.ScheduleStatus;
import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.repository.ShiftRepository;
import com.merge.backend.domain.substitute.entity.RequestStatus;
import com.merge.backend.domain.substitute.exception.SubstituteErrorCode;
import com.merge.backend.domain.substitute.repository.SubstituteRequestRepository;
import com.merge.backend.global.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubstituteRequestService {

    private final ShiftRepository shiftRepository;
    private final SubstituteRequestRepository substituteRequestRepository;

    private Shift validateSubstituteRequest(Long shiftId, Long actorUserId) {
        Shift shift = shiftRepository.findById(shiftId)
            .orElseThrow(() -> new
                BusinessException(SubstituteErrorCode.SHIFT_NOT_FOUND));

        if (shift.getSchedule().getStatus() != ScheduleStatus.PUBLISHED) {
            throw new BusinessException(SubstituteErrorCode.SHIFT_NOT_PUBLISHED);
        }

        if (!shift.getMember().getUser().getId().equals(actorUserId)) {
            throw new BusinessException(SubstituteErrorCode.NOT_OWN_SHIFT);
        }

        if (!shift.getStartAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(SubstituteErrorCode.SHIFT_ALREADY_STARTED);
        }

        if (substituteRequestRepository.existsByShift_IdAndStatusIn(
            shiftId, List.of(RequestStatus.OPEN, RequestStatus.ACCEPTED))) {
            throw new BusinessException(SubstituteErrorCode.ACTIVE_REQUEST_EXISTS);
        }

        return shift;
    }

}
