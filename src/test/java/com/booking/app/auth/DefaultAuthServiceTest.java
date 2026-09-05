package com.booking.app.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booking.app.auth.internal.application.DefaultAuthService;
import com.booking.app.auth.internal.domain.User;
import com.booking.app.auth.internal.infrastructure.UserMapper;
import com.booking.app.auth.internal.infrastructure.UserRepository;
import com.booking.app.common.security.SecurityUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class DefaultAuthServiceTest {

    private static final String EMAIL = "user@example.com";
    private static final String RAW_PASSWORD = "SecureP@ss1";
    private static final String PASSWORD_HASH = "$2a$10$somehashvalue";
    private static final String NAME = "John Doe";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new DefaultAuthService(userRepository, passwordEncoder, authenticationManager, jwtService);
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("Should register user and return UserResponse with ACTIVE status")
        void shouldRegisterUserSuccessfully() {
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(PASSWORD_HASH);
            when(userRepository.saveAndFlush(any(User.class))).thenAnswer(i -> i.getArgument(0));

            UserResponse response = authService.register(EMAIL, RAW_PASSWORD, NAME);

            assertThat(response.publicId()).isNotNull();
            assertThat(response.email()).isEqualTo(EMAIL);
            assertThat(response.name()).isEqualTo(NAME);
            assertThat(response.role()).isEqualTo(UserRole.CUSTOMER);
            assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
            verify(userRepository).saveAndFlush(any(User.class));
        }

        @Test
        @DisplayName("Should normalise email to lowercase and strip whitespace before saving")
        void shouldNormaliseEmailBeforeRegistering() {
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(PASSWORD_HASH);
            when(userRepository.saveAndFlush(any(User.class))).thenAnswer(i -> i.getArgument(0));

            UserResponse response = authService.register("  USER@Example.COM  ", RAW_PASSWORD, NAME);

            assertThat(response.email()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("Should throw EmailAlreadyExistsException when email is already registered")
        void shouldThrowEmailAlreadyExistsExceptionOnDuplicateEmail() {
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(PASSWORD_HASH);
            DataIntegrityViolationException cause =
                    new DataIntegrityViolationException("duplicate key", new RuntimeException("unique_constraint"));
            when(userRepository.saveAndFlush(any(User.class))).thenThrow(cause);

            assertThatThrownBy(() -> authService.register(EMAIL, RAW_PASSWORD, NAME))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessageContaining(EMAIL);
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("Should return AuthResponse with JWT token on valid credentials")
        void shouldLoginAndReturnToken() {
            UUID publicId = UUID.randomUUID();
            SecurityUser securityUser = new SecurityUser(
                    publicId, EMAIL, PASSWORD_HASH, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")), NAME, true);
            Authentication auth =
                    new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(jwtService.generateToken(EMAIL, "CUSTOMER", publicId)).thenReturn("jwt-token");

            AuthResponse response = authService.login(EMAIL, RAW_PASSWORD);

            assertThat(response.token()).isEqualTo("jwt-token");
            verify(jwtService).generateToken(EMAIL, "CUSTOMER", publicId);
        }

        @Test
        @DisplayName("Should normalise email to lowercase before authenticating")
        void shouldNormaliseEmailBeforeLogin() {
            UUID publicId = UUID.randomUUID();
            SecurityUser securityUser = new SecurityUser(
                    publicId, EMAIL, PASSWORD_HASH, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")), NAME, true);
            Authentication auth =
                    new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(jwtService.generateToken(anyString(), anyString(), any())).thenReturn("jwt-token");

            authService.login("  USER@EXAMPLE.COM  ", RAW_PASSWORD);

            verify(authenticationManager)
                    .authenticate(new UsernamePasswordAuthenticationToken("user@example.com", RAW_PASSWORD));
        }

        @Test
        @DisplayName("Should propagate BadCredentialsException for invalid credentials")
        void shouldPropagateBadCredentialsException() {
            when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad creds"));

            assertThatThrownBy(() -> authService.login(EMAIL, "wrong")).isInstanceOf(BadCredentialsException.class);
            verify(jwtService, never()).generateToken(anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("getProfile")
    class GetProfile {

        @Test
        @DisplayName("Should return UserResponse when user exists")
        void shouldReturnUserResponseForExistingUser() {
            UUID publicId = UUID.randomUUID();
            User user = new User(EMAIL, PASSWORD_HASH, NAME, UserRole.CUSTOMER);
            when(userRepository.findByPublicId(publicId)).thenReturn(Optional.of(user));

            UserResponse response = authService.getProfile(publicId);

            assertThat(response.email()).isEqualTo(EMAIL);
            assertThat(response.name()).isEqualTo(NAME);
            verify(userRepository).findByPublicId(publicId);
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user does not exist")
        void shouldThrowUserNotFoundExceptionForUnknownId() {
            UUID publicId = UUID.randomUUID();
            when(userRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.getProfile(publicId)).isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getAllUsers")
    class GetAllUsers {

        @Test
        @DisplayName("Should return a page of UserResponse mapped from users")
        void shouldReturnMappedPageOfUsers() {
            User user = new User(EMAIL, PASSWORD_HASH, NAME, UserRole.CUSTOMER);
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> page = new PageImpl<>(List.of(user), pageable, 1);
            when(userRepository.findAll(pageable)).thenReturn(page);

            Page<UserResponse> result = authService.getAllUsers(pageable);

            assertThat(result.getTotalElements()).isOne();
            assertThat(result.getContent().getFirst().email()).isEqualTo(EMAIL);
            verify(userRepository).findAll(pageable);
        }

        @Test
        @DisplayName("Should return an empty page when no users exist")
        void shouldReturnEmptyPageWhenNoUsersExist() {
            Pageable pageable = PageRequest.of(0, 10);
            when(userRepository.findAll(pageable)).thenReturn(Page.empty(pageable));

            Page<UserResponse> result = authService.getAllUsers(pageable);

            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("Should block user and return BLOCKED status")
        void shouldBlockActiveUser() {
            UUID publicId = UUID.randomUUID();
            User user = new User(EMAIL, PASSWORD_HASH, NAME, UserRole.CUSTOMER);
            when(userRepository.findByPublicId(publicId)).thenReturn(Optional.of(user));

            UserResponse response = authService.updateStatus(publicId, UserStatus.BLOCKED);

            assertThat(response.status()).isEqualTo(UserStatus.BLOCKED);
        }

        @Test
        @DisplayName("Should unblock user and return ACTIVE status")
        void shouldUnblockBlockedUser() {
            UUID publicId = UUID.randomUUID();
            User user = new User(EMAIL, PASSWORD_HASH, NAME, UserRole.CUSTOMER);
            user.block();
            when(userRepository.findByPublicId(publicId)).thenReturn(Optional.of(user));

            UserResponse response = authService.updateStatus(publicId, UserStatus.ACTIVE);

            assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user does not exist")
        void shouldThrowUserNotFoundExceptionForUnknownId() {
            UUID publicId = UUID.randomUUID();
            when(userRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.updateStatus(publicId, UserStatus.BLOCKED))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // Utility helper that mirrors UserMapper for verification
    @SuppressWarnings("unused")
    private static UserResponse toResponse(User user) {
        return UserMapper.toResponse(user);
    }
}
