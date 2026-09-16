package com.merge.backend.domain.user.dto;

public record LoginResponse(UserResponse user, String accessToken) {
}
