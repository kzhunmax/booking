package com.booking.app.auth.internal.web;

import com.booking.app.auth.AuthResponse;
import com.booking.app.auth.UserResponse;
import com.booking.app.common.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Authentication", description = "Endpoints for user registration, login, and profile retrieval")
@RequestMapping("/api/auth")
public interface AuthApi {

    @Operation(summary = "Register a new user", description = "Creates a new user account with regular USER role.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User successfully registered"),
        @ApiResponse(responseCode = "400", description = "Invalid payload or password requirements not met"),
        @ApiResponse(responseCode = "409", description = "Email already registered")
    })
    @PostMapping("/register")
    ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request);

    @Operation(
            summary = "Authenticate user",
            description = "Authenticates credentials and returns a signed JWT access token.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authentication successful, token returned"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Invalid email or password")
    })
    @PostMapping("/login")
    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request);

    @Operation(
            summary = "Get current user profile",
            description = "Retrieves profile information for the authenticated user based on JWT.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile returned successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token required")
    })
    @GetMapping("/me")
    ResponseEntity<UserResponse> getCurrentUser(
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser currentUser);
}
