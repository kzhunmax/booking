package com.booking.app.auth.internal.application;

import com.booking.app.auth.AuthResponse;
import com.booking.app.auth.AuthService;
import com.booking.app.auth.EmailAlreadyExistsException;
import com.booking.app.auth.JwtService;
import com.booking.app.auth.UserNotFoundException;
import com.booking.app.auth.UserResponse;
import com.booking.app.auth.UserRole;
import com.booking.app.auth.UserStatus;
import com.booking.app.auth.internal.domain.User;
import com.booking.app.auth.internal.infrastructure.UserMapper;
import com.booking.app.auth.internal.infrastructure.UserRepository;
import com.booking.app.common.security.SecurityUser;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            String token = jwtService.generateToken(user.getUsername(), user.role(), user.publicId());
            return new AuthResponse(token);
        }
        throw new IllegalStateException("Unexpected principle type");
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID publicId) {
        return UserMapper.toResponse(requireUser(publicId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserMapper::toResponse);
    }

    @Override
    @Transactional
    public UserResponse updateStatus(UUID publicId, UserStatus status) {
        User user = requireUser(publicId);
        switch (status) {
            case ACTIVE -> user.unblock();
            case BLOCKED -> user.block();
        }
        return UserMapper.toResponse(user);
    }

    private User requireUser(UUID publicId) {
        return userRepository.findByPublicId(publicId).orElseThrow(() -> new UserNotFoundException(publicId));
    }

    private static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
