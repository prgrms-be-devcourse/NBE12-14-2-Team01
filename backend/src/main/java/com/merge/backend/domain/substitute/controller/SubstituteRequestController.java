package com.merge.backend.domain.substitute.controller;

import com.merge.backend.domain.substitute.dto.response.ReceivedSubstituteRequestResponse;
import com.merge.backend.domain.substitute.service.SubstituteCandidateService;
import com.merge.backend.global.dto.ApiResponse;
import com.merge.backend.global.rq.Rq;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/substitute-requests")
public class SubstituteRequestController {

    private final SubstituteCandidateService substituteCandidateService;
    private final Rq rq;

    @GetMapping("/received")
    public ResponseEntity<ApiResponse<List<ReceivedSubstituteRequestResponse>>> getReceivedRequests() {

        Long userId = rq.getActorId(); // 현재 로그인한 User의 ID

        List<ReceivedSubstituteRequestResponse> response =
            substituteCandidateService
                .findReceivedRequests(userId) // Service에서 응답 가능한 Candidate 조회
                .stream()
                .map(ReceivedSubstituteRequestResponse::from) // Entity → 응답 DTO
                .toList();

        return ResponseEntity.ok(
            ApiResponse.success(
                "200",
                "응답 가능한 대타 요청을 조회했습니다.",
                response
            )
        );
    }
}