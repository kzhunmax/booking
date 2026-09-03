package com.booking.app.auth.internal.infrastructure;

import com.booking.app.auth.UserResponse;
import com.booking.app.auth.internal.domain.User;

public final class UserMapper {

    private UserMapper() {}

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getPublicId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
