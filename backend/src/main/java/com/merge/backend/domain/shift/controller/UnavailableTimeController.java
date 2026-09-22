package com.merge.backend.domain.shift.controller;

import com.merge.backend.domain.shift.dto.*;
import com.merge.backend.domain.shift.entity.UnavailableTime;
import com.merge.backend.domain.shift.service.UnavailableTimeService;
import com.merge.backend.global.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/unavailable-times")
public class UnavailableTimeController {

    private final UnavailableTimeService unavailableTimeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UnavailableTimeListResponse>>> list() {
        List<UnavailableTimeService.UnavailableTimeResult> results =
                unavailableTimeService.findAll();

        List<UnavailableTimeListResponse> responses = results.stream()
                .map(result -> new UnavailableTimeListResponse(
                        result.unavailableTimeId(),
                        result.startAt(),
                        result.endAt(),
                        result.officialShiftConflict()
                ))
                .toList();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "200",
                        "근무 불가능 일정을 조회했습니다.",
                        responses
                )
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UnavailableTimeRegisterResponse>> register(
            @Valid @RequestBody UnavailableTimeRegisterReqBody reqBody
            ) {

        UnavailableTime savedUnavailableTime =
                unavailableTimeService.register(reqBody.startAt(),
                        reqBody.endAt(),
                        reqBody.confirmOfficialShiftConflict());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "201",
                        "근무 불가능 일정이 등록되었습니다",
                        new UnavailableTimeRegisterResponse(
                                savedUnavailableTime.getId(),
                                savedUnavailableTime.getStartAt(),
                                savedUnavailableTime.getEndAt()
                        )
                ));

    }

    @PatchMapping("/{unavailableTimeId}")
    public ResponseEntity<ApiResponse<UnavailableTimeUpdateResponse>> update(
            @PathVariable Long unavailableTimeId,
            @Valid @RequestBody UnavailableTimeUpdateReqBody reqBody
            ) {
        UnavailableTime unavailableTime =
                unavailableTimeService.update(
                        unavailableTimeId,
                        reqBody.startAt(),
                        reqBody.endAt(),
                        reqBody.confirmOfficialShiftConflict()
                );

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(
                                "200",
                                "근무 불가능 일정이 수정되었습니다",
                                new UnavailableTimeUpdateResponse(
                                        unavailableTime.getId(),
                                        unavailableTime.getStartAt(),
                                        unavailableTime.getEndAt()
                                )
                        )
                );
    }

    @DeleteMapping("/{unavailableTimeId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long unavailableTimeId
    ) {

        unavailableTimeService.delete(unavailableTimeId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "200",
                        "근무 불가능 일정이 삭제되었습니다."
                )
        );
    }
}
