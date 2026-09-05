package com.booking.app.auth;

import java.util.UUID;
import org.springframework.security.core.userdetails.UserDetails;

public interface JwtService {
    String generateToken(String subject, String role, UUID userId);

    String extractSubject(String token);

    String extractRole(String token);

    UUID extractUserId(String token);

    boolean isValid(String token, UserDetails userDetails);
}
