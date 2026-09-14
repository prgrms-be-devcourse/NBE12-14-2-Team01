package com.merge.backend.domain.shift.controller;

import com.merge.backend.domain.shift.entity.RegularShiftPattern;
import com.merge.backend.domain.shift.entity.Shift;
import com.merge.backend.domain.shift.service.ShiftService;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("api/v1/shift")
public class ShiftController {

    private final ShiftService shiftService;

    @PostMapping
    public ResponseEntity<?> create(
        @RequestBody RegularShiftPattern reqBody,
        @RequestParam int scheduleId
    ){
        Shift shift = shiftService.create(reqBody, scheduleId);

        return ResponseEntity.status(201).body(ApiRe);
    }

}
