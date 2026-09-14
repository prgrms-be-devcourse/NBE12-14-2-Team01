package com.merge.backend.domain.user.controller;

import com.merge.backend.domain.user.dto.SignupRequest;
import com.merge.backend.domain.user.entity.User;
import com.merge.backend.domain.user.service.UserService;
import com.merge.backend.global.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class ApiV1UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<User>> signup(@Valid @RequestBody SignupRequest request) {
        User user = userService.join(request.email(), request.password(), request.name());

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success("201", user));
    }

}
