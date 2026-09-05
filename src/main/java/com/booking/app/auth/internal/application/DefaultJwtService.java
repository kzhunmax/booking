package com.booking.app.auth.internal.application;

import com.booking.app.auth.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class DefaultJwtService implements JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;

    public DefaultJwtService(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration-ms}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    @Override
    public String generateToken(String subject, String role, UUID userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .claim("userId", userId.toString())
                .claim("role", role)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    @Override
    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    @Override
    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    @Override
    public UUID extractUserId(String token) {
        String userId = parseClaims(token).get("userId", String.class);
        return UUID.fromString(userId);
    }

    @Override
    public boolean isValid(String token, UserDetails userDetails) {
        String subject = extractSubject(token);

        return subject.equals(userDetails.getUsername()) && !isExpired(token);
    }

    private boolean isExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
