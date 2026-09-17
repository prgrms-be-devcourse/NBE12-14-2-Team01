package com.merge.backend.domain.shift.controller;

import com.merge.backend.domain.shift.dto.ShiftCreateResponse;
import com.merge.backend.domain.shift.dto.ShiftDetailResponse;
import com.merge.backend.domain.shift.dto.ShiftModifyResponse;
import com.merge.backend.domain.shift.dto.ShiftRequest;
import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.service.ShiftService;
import com.merge.backend.global.dto.ApiResponse;
import com.merge.backend.global.rq.Rq;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ShiftController {

    private final ShiftService shiftService;
    private final Rq rq;

    @GetMapping("/shifts/{shiftId}")
    public ResponseEntity<ApiResponse<ShiftDetailResponse>> detail(
        @PathVariable Long shiftId
    ){
        Long currentUserId = rq.getActorId();

        Shift shift = shiftService.detail(shiftId, currentUserId);

        return ResponseEntity.status(200).body(
            ApiResponse.success(
                "200",
                ShiftDetailResponse.from(shift))
        );
    }

    @PostMapping("/workplaces/{workplaceId}/schedules/{scheduleId}/shifts")
    public ResponseEntity<ApiResponse<ShiftCreateResponse>> create(
        @PathVariable Long workplaceId,
        @PathVariable Long scheduleId,
        @Valid @RequestBody ShiftRequest reqBody
    ){
        Shift shift = shiftService.create(reqBody, workplaceId, scheduleId, rq.getActorId());

        return ResponseEntity.status(201).body(
            ApiResponse.success(
                "201",
                "근무가 추가되었습니다.",
                ShiftCreateResponse.from(shift))
        );
    }

    @PatchMapping("/workplaces/{workplaceId}/schedules/{scheduleId}/shifts/{shiftId}")
    public ResponseEntity<ApiResponse<ShiftModifyResponse>> modify(
        @PathVariable Long workplaceId,
        @PathVariable Long scheduleId,
        @PathVariable Long shiftId,
        @Valid @RequestBody ShiftRequest reqBody
    ){

        Shift modifiedShift = shiftService.modify(
            workplaceId,
            scheduleId,
            shiftId,
            reqBody,
            rq.getActorId()
        );
        return ResponseEntity.status(200).body(
            ApiResponse.success(
                "200",
                "근무가 수정되었습니다.",
                ShiftModifyResponse.from(modifiedShift))
        );
    }

}
