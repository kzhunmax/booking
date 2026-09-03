package com.booking.app.auth;

import java.util.UUID;

public interface AuthService {
    UserResponse register(String email, String password, String name);

    AuthResponse login(String email, String password);

    UserResponse getProfile(UUID publicId);
}
