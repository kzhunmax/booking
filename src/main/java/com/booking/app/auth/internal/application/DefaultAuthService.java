package com.booking.app.auth.internal.application;

import com.booking.app.auth.AuthResponse;
import com.booking.app.auth.AuthService;
import com.booking.app.auth.EmailAlreadyExistsException;
import com.booking.app.auth.JwtService;
import com.booking.app.auth.UserNotFoundException;
import com.booking.app.auth.UserResponse;
import com.booking.app.auth.UserRole;
import com.booking.app.auth.internal.domain.User;
import com.booking.app.auth.internal.infrastructure.UserMapper;
import com.booking.app.auth.internal.infrastructure.UserRepository;
import com.booking.app.common.security.SecurityUser;
import jakarta.transaction.Transactional;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class DefaultAuthService implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public DefaultAuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public UserResponse register(String email, String password, String name) {
        String normalizedEmail = normalizeEmail(email);
        User user = new User(normalizedEmail, passwordEncoder.encode(password), name, UserRole.CUSTOMER);
        try {
            userRepository.saveAndFlush(user);
            return UserMapper.toResponse(user);
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyExistsException(email, e);
        }
    }

    @Override
    public AuthResponse login(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        Authentication authentication =
                authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(normalizedEmail, password));
        if (authentication.getPrincipal() instanceof SecurityUser user) {
            String token =
                    jwtService.generateToken(user.getUsername(), user.role().name(), user.publicId());
            return new AuthResponse(token);
        }
        throw new IllegalStateException("Unexpected principle type");
    }

    @Override
    public UserResponse getProfile(UUID publicId) {
        User user = userRepository.findByPublicId(publicId).orElseThrow(() -> new UserNotFoundException(publicId));
        return UserMapper.toResponse(user);
    }

    private static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
