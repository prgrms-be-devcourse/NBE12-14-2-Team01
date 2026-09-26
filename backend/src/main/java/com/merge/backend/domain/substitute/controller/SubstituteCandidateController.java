package com.merge.backend.domain.substitute.controller;

import com.merge.backend.domain.substitute.dto.SubstituteCandidateRespondRequest;
import com.merge.backend.domain.substitute.dto.SubstituteCandidateRespondResponse;
import com.merge.backend.domain.substitute.entity.SubstituteCandidate;
import com.merge.backend.domain.substitute.service.SubstituteCandidateService;
import com.merge.backend.global.dto.ApiResponse;
import com.merge.backend.global.rq.Rq;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/substitute-candidates")
public class SubstituteCandidateController {

    private final SubstituteCandidateService substituteCandidateService;
    private final Rq rq;

    @PatchMapping("/{candidateId}/response")
    public ResponseEntity<ApiResponse<SubstituteCandidateRespondResponse>> respond(
        @PathVariable Long candidateId, // 응답할 Candidate ID
        @Valid @RequestBody SubstituteCandidateRespondRequest request
    ) {
        Long userId = rq.getActorId(); // 현재 로그인한 User ID

        SubstituteCandidate candidate;

        if (request.decision() == SubstituteCandidateRespondRequest.Decision.ACCEPT) {

            // 사용자가 수락을 선택한 경우
            candidate = substituteCandidateService.acceptCandidate(
                candidateId,
                userId
            );

        } else {

            // 사용자가 거절을 선택한 경우
            candidate = substituteCandidateService.rejectCandidate(
                candidateId,
                userId
            );
        }
        // Service가 반환한 Entity를 API 응답 DTO로 변환
        SubstituteCandidateRespondResponse response =
            SubstituteCandidateRespondResponse.from(candidate);

        String message =
            request.decision()
                == SubstituteCandidateRespondRequest.Decision.ACCEPT
                ? "대타 요청을 수락했습니다."
                : "대타 요청을 거절했습니다.";

        return ResponseEntity.ok(
            ApiResponse.success(
                "200",
                message,
                response
            )
        );
    }
}