package com.merge.backend.domain.shift.controller;

import com.merge.backend.domain.shift.dto.ShiftCreateResponse;
import com.merge.backend.domain.shift.dto.ShiftModifyResponse;
import com.merge.backend.domain.shift.dto.ShiftRequest;
import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.service.ShiftService;
import com.merge.backend.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workplaces/{workplaceId}/schedules/{scheduleId}/shifts")
public class ShiftController {

    private final ShiftService shiftService;

    @PostMapping
    public ResponseEntity<ApiResponse<ShiftCreateResponse>> create(
        @PathVariable Long workplaceId,
        @PathVariable Long scheduleId,
        @RequestBody ShiftRequest reqBody
    ){
        Shift shift = shiftService.create(reqBody, workplaceId, scheduleId);

        return ResponseEntity.status(201).body(
            ApiResponse.success(
                "201",
                "근무가 추가되었습니다.",
                ShiftCreateResponse.from(shift))
        );
    }

    @PatchMapping("/{shiftId}")
    public ResponseEntity<ApiResponse<ShiftModifyResponse>> modify(
        @PathVariable Long workplaceId,
        @PathVariable Long scheduleId,
        @PathVariable Long shiftId,
        @RequestBody ShiftRequest reqBody
    ){

        Shift modifiedShift = shiftService.modify(
            workplaceId,
            scheduleId,
            shiftId,
            reqBody
        );
        return ResponseEntity.status(200).body(
            ApiResponse.success(
                "200",
                "근무가 수정되었습니다.",
                ShiftModifyResponse.from(modifiedShift))
        );
    }

}
