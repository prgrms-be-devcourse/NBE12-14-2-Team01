package com.merge.backend.domain.shift.controller;

import com.merge.backend.domain.shift.dto.ShiftRequest;
import com.merge.backend.domain.shift.dto.ShiftResponse;
import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.service.ShiftService;
import com.merge.backend.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/v1/workplaces/{workplaceId}/schedules/{scheduleId}/shifts")
public class ShiftController {

    private final ShiftService shiftService;

    @PostMapping
    public ResponseEntity<ApiResponse<ShiftResponse>> create(
        @PathVariable Long workplaceId,
        @PathVariable Long scheduleId,
        @RequestBody ShiftRequest reqBody
    ){
        Shift shift = shiftService.create(reqBody, workplaceId, scheduleId);

        return ResponseEntity.status(201).body(
            ApiResponse.success(
                "201",
                "근무가 추가되었습니다.",
                ShiftResponse.from(shift))
        );
    }

}
