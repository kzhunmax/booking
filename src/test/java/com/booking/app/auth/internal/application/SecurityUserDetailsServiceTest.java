package com.booking.app.auth.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.booking.app.auth.UserRole;
import com.booking.app.auth.internal.domain.User;
import com.booking.app.auth.internal.infrastructure.UserRepository;
import com.booking.app.common.security.SecurityUser;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class SecurityUserDetailsServiceTest {

    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD_HASH = "$2a$10$hash";
    private static final String NAME = "John Doe";

    @Mock
    private UserRepository userRepository;

    private SecurityUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new SecurityUserDetailsService(userRepository);
    }

    @Test
    @DisplayName("Should return SecurityUser with correct fields for a known active user")
    void shouldReturnSecurityUserForKnownEmail() {
        User user = new User(EMAIL, PASSWORD_HASH, NAME, UserRole.CUSTOMER);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername(EMAIL);

        assertThat(details).isInstanceOf(SecurityUser.class);
        SecurityUser securityUser = (SecurityUser) details;
        assertThat(securityUser.getUsername()).isEqualTo(EMAIL);
        assertThat(securityUser.getPassword()).isEqualTo(PASSWORD_HASH);
        assertThat(securityUser.role()).isEqualTo("CUSTOMER");
        assertThat(securityUser.name()).isEqualTo(NAME);
        assertThat(securityUser.isEnabled()).isTrue();
        assertThat(securityUser.getAuthorities()).extracting("authority").containsExactly("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("Should return SecurityUser for ADMIN role with ROLE_ADMIN authority")
    void shouldReturnAdminSecurityUserWithCorrectAuthority() {
        User admin = new User(EMAIL, PASSWORD_HASH, NAME, UserRole.ADMIN);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(admin));

        SecurityUser securityUser = (SecurityUser) service.loadUserByUsername(EMAIL);

        assertThat(securityUser.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
        assertThat(securityUser.isAdmin()).isTrue();
    }

    @Test
    @DisplayName("Should return disabled SecurityUser when user is BLOCKED")
    void shouldReturnDisabledSecurityUserWhenBlocked() {
        User user = new User(EMAIL, PASSWORD_HASH, NAME, UserRole.CUSTOMER);
        user.block();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        SecurityUser securityUser = (SecurityUser) service.loadUserByUsername(EMAIL);

        assertThat(securityUser.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Should normalise email to lowercase before querying repository")
    void shouldNormaliseEmailBeforeLookup() {
        User user = new User(EMAIL, PASSWORD_HASH, NAME, UserRole.CUSTOMER);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("  USER@EXAMPLE.COM  ");

        assertThat(details.getUsername()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException for an unknown email")
    void shouldThrowUsernameNotFoundExceptionForUnknownEmail() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername(EMAIL))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining(EMAIL);
    }
}
