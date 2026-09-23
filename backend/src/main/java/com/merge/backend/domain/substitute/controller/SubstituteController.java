package com.merge.backend.domain.substitute.controller;

import com.merge.backend.domain.substitute.dto.response.SubstituteRequestCreateResponse;
import com.merge.backend.domain.substitute.service.SubstituteRequestService;
import com.merge.backend.global.dto.ApiResponse;
import com.merge.backend.global.rq.Rq;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SubstituteController {

    private final SubstituteRequestService substituteRequestService;
    private final Rq rq;

    @PostMapping("/shifts/{shiftId}/substitute-requests")
    public ResponseEntity<ApiResponse<SubstituteRequestCreateResponse>> create(
        @PathVariable Long shiftId
    ) {
        SubstituteRequestCreateResponse response =
            substituteRequestService.create(shiftId, rq.getActorId());

        if (response.requestCreated()) {
            return ResponseEntity.status(201).body(
                ApiResponse.success(
                    "201",
                    "대타 요청이 생성되었습니다.",
                    response
                )
            );
        }

        return ResponseEntity.status(200).body(
            ApiResponse.success(
                "200",
                "현재 대타 요청을 보낼 수 있는 후보가 없습니다.",
                response
            )
        );
    }
}