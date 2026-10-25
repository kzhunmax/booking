package com.booking.app.auth.internal.web;

import com.booking.app.auth.AuthResponse;
import com.booking.app.auth.AuthService;
import com.booking.app.auth.UserResponse;
import com.booking.app.common.security.SecurityUser;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
public class AuthController implements AuthApi {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public ResponseEntity<UserResponse> register(RegisterRequest request) {
        UserResponse created = authService.register(request.email(), request.password(), request.name());
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/users/{id}")
                .buildAndExpand(created.publicId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Override
    public ResponseEntity<AuthResponse> login(LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.email(), request.password()));
    }

    @Override
    public ResponseEntity<UserResponse> getCurrentUser(SecurityUser currentUser) {
        return ResponseEntity.ok(authService.getProfile(currentUser.publicId()));
    }
}
