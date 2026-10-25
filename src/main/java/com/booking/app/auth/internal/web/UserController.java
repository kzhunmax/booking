package com.booking.app.auth.internal.web;

import com.booking.app.auth.AuthService;
import com.booking.app.auth.UserResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController implements UserApi {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserResponse> findAllUsers(Pageable pageable) {
        return authService.getAllUsers(pageable);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateStatus(UUID publicId, UpdateUserStatusRequest request) {
        return ResponseEntity.ok(authService.updateStatus(publicId, request.status()));
    }
}
