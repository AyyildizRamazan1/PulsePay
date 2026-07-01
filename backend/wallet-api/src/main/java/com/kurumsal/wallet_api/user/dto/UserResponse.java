package com.kurumsal.wallet_api.user.dto;

import com.kurumsal.wallet_api.user.domain.User;
import com.kurumsal.wallet_api.user.domain.UserStatus;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String name,
        String phone,
        UserStatus status,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}
