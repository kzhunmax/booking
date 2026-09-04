package com.booking.app.auth;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuthService {
    UserResponse register(String email, String password, String name);

    AuthResponse login(String email, String password);

    UserResponse getProfile(UUID publicId);

    Page<UserResponse> getAllUsers(Pageable pageable);

    UserResponse updateStatus(UUID publicId, UserStatus status);
}
