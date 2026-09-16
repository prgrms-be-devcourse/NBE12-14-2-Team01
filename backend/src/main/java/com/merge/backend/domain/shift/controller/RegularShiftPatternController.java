package com.merge.backend.domain.shift.controller;

import com.merge.backend.domain.shift.dto.RegularShiftPatternListResponse;
import com.merge.backend.domain.shift.dto.RegularShiftPatternReqBody;
import com.merge.backend.domain.shift.dto.RegularShiftPatternResponse;
import com.merge.backend.domain.shift.entity.RegularShiftPattern;
import com.merge.backend.domain.shift.service.RegularShiftPatternService;
import com.merge.backend.global.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/workplaces/{workplaceId}/regular-shift-patterns")
public class RegularShiftPatternController {

    private final RegularShiftPatternService regularShiftPatternService;

    @GetMapping
    public ApiResponse<List<RegularShiftPatternListResponse>> list(
            @PathVariable Long workplaceId
    ) {
        List<RegularShiftPatternListResponse> regularShiftPatternList = regularShiftPatternService.findAll(workplaceId);

        return ApiResponse.success(
                "200",
                regularShiftPatternList
        );
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("")
    public ApiResponse<RegularShiftPatternResponse> register(
            @PathVariable Long workplaceId,
            @Valid @RequestBody RegularShiftPatternReqBody reqBody
            ) {

        RegularShiftPatternResponse newRegularShiftPattern = regularShiftPatternService.register(
                workplaceId,
                reqBody);

        return ApiResponse.success(
                "201",
                "정기 근무가 등록되었습니다.",
                newRegularShiftPattern
        );


    }

    @PutMapping("/{patternId}")
    public ApiResponse<RegularShiftPatternResponse> update(
            @PathVariable Long workplaceId,
            @PathVariable Long patternId,
            @Valid @RequestBody RegularShiftPatternReqBody reqBody
    ) {
        RegularShiftPatternResponse updatedRegularShiftPattern = regularShiftPatternService.update(workplaceId, patternId, reqBody);

        return ApiResponse.success(
                "200",
                "정기 근무가 수정되었습니다",
                updatedRegularShiftPattern
        );
    }

    @DeleteMapping("/{patternId}")
    public ApiResponse<Void> delete(
            @PathVariable Long workplaceId,
            @PathVariable Long patternId
    ) {
        regularShiftPatternService.delete(workplaceId, patternId);

        return ApiResponse.success(
                "200",
                "%d번 정기 근무가 삭제되었습니다".formatted(patternId)
        );
    }

}
