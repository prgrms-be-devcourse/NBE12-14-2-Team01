package com.merge.backend.domain.shift.controller;

import com.merge.backend.domain.shift.dto.ManagerScheduleResponse;
import com.merge.backend.domain.shift.dto.ScheduleCreateRequest;
import com.merge.backend.domain.shift.dto.ScheduleCreateResponse;
import com.merge.backend.domain.shift.dto.SchedulePublishRequest;
import com.merge.backend.domain.shift.dto.SchedulePublishResponse;
import com.merge.backend.domain.shift.service.ScheduleService;
import com.merge.backend.global.dto.ApiResponse;
import com.merge.backend.global.rq.Rq;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/workplaces/{workplaceId}/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final Rq rq;

    @PostMapping
    public ResponseEntity<ApiResponse<ScheduleCreateResponse>> createDraftSchedule(
        @PathVariable Long workplaceId,
        @Valid @RequestBody ScheduleCreateRequest request
    ) {

        Long actorUserId = rq.getActorId();

        ScheduleCreateResponse response =
            scheduleService.createDraftSchedule(
                actorUserId,
                workplaceId,
                request.weekStartDate()
            );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                ApiResponse.success(
                    "201",
                    "주간 근무표 초안이 생성되었습니다.",
                    response
                )
            );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ManagerScheduleResponse>> getWeeklySchedule(
        @PathVariable Long workplaceId,
        @RequestParam LocalDate weekStartDate
    ) {

        Long actorUserId = rq.getActorId();

        ManagerScheduleResponse response =
            scheduleService.getWeeklySchedule(
                actorUserId,
                workplaceId,
                weekStartDate
            );

        if (response == null) {
            return ResponseEntity.ok(
                ApiResponse.success(
                    "200",
                    "해당 주차의 근무표가 아직 생성되지 않았습니다.",
                    null
                )
            );
        }

        return ResponseEntity.ok(
            ApiResponse.success(
                "200",
                "주간 근무표를 조회했습니다.",
                response
            )
        );
    }

    @PatchMapping("/{scheduleId}/publish")
    public ResponseEntity<ApiResponse<SchedulePublishResponse>> publishSchedule(
        @PathVariable Long workplaceId,
        @PathVariable Long scheduleId,
        @Valid @RequestBody SchedulePublishRequest request
    ) {
        Long actorUserId =
            rq.getActorId();

        SchedulePublishResponse response =
            scheduleService.publishSchedule(
                actorUserId,
                workplaceId,
                scheduleId,
                request.confirmUnavailableConflict()
            );

        return ResponseEntity.ok(
            ApiResponse.success(
                "200",
                "근무표가 공개되었습니다.",
                response
            )
        );
    }

}