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
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

    // TODO: auth-login PR 머지 후 UserRepository 대신 Rq 주입
    // private final Rq rq;

    @PostMapping
    public ResponseEntity<ApiResponse<WorkplaceCreateResponse>> createWorkplace(
        @Valid @RequestBody WorkplaceCreateRequest request,
        Authentication authentication
    ) {
        // TODO: auth-login PR 머지 후 User user = rq.getActor(); 로 변경
        User user = userRepository.findByEmail(authentication.getName())
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
        @Valid @RequestBody WorkplaceJoinRequest request,
        Authentication authentication
    ) {
        // TODO: auth-login PR 머지 후 User user = rq.getActor(); 로 변경
        User user = userRepository.findByEmail(authentication.getName())
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
    public ResponseEntity<ApiResponse<List<MyWorkplaceResponse>>> getMyWorkplaces(
        Authentication authentication
    ) {
        // TODO: auth-login PR 머지 후 User user = rq.getActor(); 로 변경
        User user = userRepository.findByEmail(authentication.getName())
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