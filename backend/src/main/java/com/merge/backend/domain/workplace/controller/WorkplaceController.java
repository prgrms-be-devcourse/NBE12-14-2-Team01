package com.merge.backend.domain.workplace.controller;

import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.user.repository.UserRepository;
import com.merge.backend.domain.workplace.dto.request.WorkplaceCreateRequest;
import com.merge.backend.domain.workplace.dto.request.WorkplaceJoinRequest;
import com.merge.backend.domain.workplace.dto.response.MyWorkplaceResponse;
import com.merge.backend.domain.workplace.dto.response.WorkplaceCreateResponse;
import com.merge.backend.domain.workplace.dto.response.WorkplaceJoinResponse;
import com.merge.backend.domain.workplace.service.WorkplaceService;
import com.merge.backend.global.dto.ApiResponse;
import com.merge.backend.global.rq.Rq;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/workplaces")
public class WorkplaceController {

    private final WorkplaceService workplaceService;
    private final UserRepository userRepository;
    private final Rq rq;

    @PostMapping
    public ResponseEntity<ApiResponse<WorkplaceCreateResponse>> createWorkplace(
        @Valid @RequestBody WorkplaceCreateRequest request
    ) {
        Long userId = rq.getActorId();

        User user = userRepository.findById(userId)
            .orElseThrow();

        WorkplaceCreateResponse response =
            workplaceService.createWorkplace(request, user);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(
                "201",
                "Workplace가 생성되었습니다.",
                response
            ));
    }

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<WorkplaceJoinResponse>> joinWorkplace(
        @Valid @RequestBody WorkplaceJoinRequest request
    ) {
        Long userId = rq.getActorId();

        User user = userRepository.findById(userId)
            .orElseThrow();

        WorkplaceJoinResponse response =
            workplaceService.joinWorkplace(request, user);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(
                "201",
                "Workplace 참여가 완료되었습니다.",
                response
            ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MyWorkplaceResponse>>> getMyWorkplaces() {
        Long userId = rq.getActorId();

        User user = userRepository.findById(userId)
            .orElseThrow();

        List<MyWorkplaceResponse> response =
            workplaceService.getMyWorkplaces(user);

        return ResponseEntity.ok(ApiResponse.success(
            "200",
            "소속 Workplace 목록을 조회했습니다.",
            response
        ));
    }
}