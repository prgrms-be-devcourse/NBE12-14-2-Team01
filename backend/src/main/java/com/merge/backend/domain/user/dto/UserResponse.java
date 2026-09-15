package com.merge.backend.domain.user.dto;

import com.merge.backend.domain.user.entity.User;

public record UserResponse(Long id, String email, String name) {

    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getName()
        );
    }
}
