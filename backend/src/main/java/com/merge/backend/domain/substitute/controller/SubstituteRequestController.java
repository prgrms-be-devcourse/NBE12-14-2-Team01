package com.merge.backend.domain.substitute.controller;

import com.merge.backend.domain.substitute.dto.SubstituteRequestResponse;
import com.merge.backend.domain.substitute.entity.SubstituteRequest;
import com.merge.backend.domain.substitute.service.SubstituteRequestService;
import com.merge.backend.global.dto.ApiResponse;
import com.merge.backend.global.rq.Rq;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SubstituteRequestController {

    private final Rq rq;
    private final SubstituteRequestService substituteRequestService;

    @PatchMapping("substitute-requests/{requestId}/approve")
    public ResponseEntity<ApiResponse<SubstituteRequestResponse>> approve(
        @PathVariable Long requestId
    ) {
        Long actorId = rq.getActorId();
        SubstituteRequest request = substituteRequestService.approve(requestId, actorId);

        return ResponseEntity.status(200).body(
            ApiResponse.success(
                "200",
                "대타 요청이 최종 승인되었습니다.",
                SubstituteRequestResponse.from(request)
            )
        );
    }

}
