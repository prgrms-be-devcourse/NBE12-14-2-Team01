package com.merge.backend.domain.substitute.controller;

import com.merge.backend.domain.substitute.dto.SubstituteRequestListResponse;
import com.merge.backend.domain.substitute.dto.SubstituteRequestResponse;
import com.merge.backend.domain.substitute.entity.RequestStatus;
import com.merge.backend.domain.substitute.entity.SubstituteCandidate;
import com.merge.backend.domain.substitute.entity.SubstituteRequest;
import com.merge.backend.domain.substitute.service.SubstituteRequestService;
import com.merge.backend.global.dto.ApiResponse;
import com.merge.backend.global.rq.Rq;
import java.util.List;
import java.util.Map;
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
    private final SubstituteRequestService substituteRequestService;

    @GetMapping("/workplaces/{workplaceId}/substitute-requests/ongoing")
    public ResponseEntity<ApiResponse<List<SubstituteRequestListResponse>>> list(
        @PathVariable Long workplaceId
    ){
        Long actorId = rq.getActorId();

        //대체근무 요청 리스트
        List<SubstituteRequest> requests = substituteRequestService.list(workplaceId, actorId);

        //대체근무 요청 리스트를 가져온 후, ACCEPTED중인 근무를 선별
        List<Long> acceptedRequestIds = requests.stream()
            .filter(r -> r.getStatus() == RequestStatus.ACCEPTED)
            .map(SubstituteRequest::getId)
            .toList();

        //수락자의 정보를 가져옴 (요청 리스트, 수락자 정보 한 번에 가져오고 싶지만 엔티티 클래스를
        //변경하지 않는다면 따로 가져와야함
        Map<Long, SubstituteCandidate> acceptedMap =
            substituteRequestService.getAcceptedCandidates(acceptedRequestIds);

        return ResponseEntity.status(200).body(
            ApiResponse.success(
                "200",
                "진행 중인 대타 요청을 조회했습니다.",
                requests.stream()
                    .map(request ->
                        SubstituteRequestListResponse.from(request, acceptedMap))
                    .toList()
            )
        );
    }

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
