package com.merge.backend.domain.substitute.controller;

import com.merge.backend.domain.substitute.dto.AcceptedPendingSubstituteRequestResponse;
import com.merge.backend.domain.substitute.dto.ReceivedSubstituteRequestResponse;
import com.merge.backend.domain.substitute.dto.SubstituteRequestCloseResponse;
import com.merge.backend.domain.substitute.dto.SubstituteRequestListResponse;
import com.merge.backend.domain.substitute.dto.SubstituteRequestResponse;
import com.merge.backend.domain.substitute.entity.SubstituteRequest;
import com.merge.backend.domain.substitute.service.SubstituteCandidateService;
import com.merge.backend.domain.substitute.service.SubstituteRequestService;
import com.merge.backend.global.dto.ApiResponse;
import com.merge.backend.global.rq.Rq;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SubstituteRequestController {

    private final Rq rq;
    private final SubstituteCandidateService substituteCandidateService;
    private final SubstituteRequestService substituteRequestService;

    // SUB-02 - 내가 받은 응답 가능한 대타 요청 조회
    @GetMapping("/substitute-requests/received")
    public ResponseEntity<ApiResponse<List<ReceivedSubstituteRequestResponse>>>
        getReceivedRequests() {

        Long userId = rq.getActorId();

        List<ReceivedSubstituteRequestResponse> response =
            substituteCandidateService
                .findReceivedRequests(userId)
                .stream()
                .map(ReceivedSubstituteRequestResponse::from)
                .toList();

        return ResponseEntity.ok(
            ApiResponse.success(
                "200",
                "응답 가능한 대타 요청을 조회했습니다.",
                response
            )
        );
    }

    // SUB-07 - 내가 수락한 승인 대기 대타 요청 조회
    @GetMapping("/substitute-requests/accepted")
    public ResponseEntity<ApiResponse<List<AcceptedPendingSubstituteRequestResponse>>>
        getAcceptedPendingRequests() {

        Long userId = rq.getActorId();

        List<AcceptedPendingSubstituteRequestResponse> response =
            substituteCandidateService
                .findAcceptedPendingRequests(userId)
                .stream()
                .map(AcceptedPendingSubstituteRequestResponse::from)
                .toList();

        return ResponseEntity.ok(
            ApiResponse.success(
                "200",
                "승인 대기 중인 대타 요청을 조회했습니다.",
                response
            )
        );
    }

    // MANAGER - 진행 중인 대타 요청 조회
    @GetMapping("/workplaces/{workplaceId}/substitute-requests/ongoing")
    public ResponseEntity<ApiResponse<List<SubstituteRequestListResponse>>> list(
        @PathVariable Long workplaceId
    ) {
        Long actorId = rq.getActorId();

        List<SubstituteRequestListResponse> response =
            substituteRequestService.list(workplaceId, actorId);

        return ResponseEntity.status(200).body(
            ApiResponse.success(
                "200",
                "진행 중인 대타 요청을 조회했습니다.",
                response
            )
        );
    }

    // MANAGER - 대타 요청 최종 승인
    @PatchMapping("/substitute-requests/{requestId}/approve")
    public ResponseEntity<ApiResponse<SubstituteRequestResponse>> approve(
        @PathVariable Long requestId
    ) {
        Long actorId = rq.getActorId();

        SubstituteRequest request =
            substituteRequestService.approve(requestId, actorId);

        return ResponseEntity.status(200).body(
            ApiResponse.success(
                "200",
                "대타 요청이 최종 승인되었습니다.",
                SubstituteRequestResponse.from(request)
            )
        );
    }

    @PatchMapping("/substitute-requests/{requestId}/close")
    public ResponseEntity<ApiResponse<SubstituteRequestCloseResponse>> close(
        @PathVariable Long requestId
    ) {
        Long actorId = rq.getActorId();

        SubstituteRequest request =
            substituteRequestService.close(requestId, actorId);

        return ResponseEntity.status(200).body(
            ApiResponse.success(
                "200",
                "대타 요청이 종료되었습니다.",
                SubstituteRequestCloseResponse.from(request)
            )
        );
    }
}