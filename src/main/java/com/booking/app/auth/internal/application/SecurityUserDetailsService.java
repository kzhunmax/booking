package com.booking.app.auth.internal.application;

import com.booking.app.auth.UserRole;
import com.booking.app.auth.UserStatus;
import com.booking.app.auth.internal.domain.User;
import com.booking.app.auth.internal.infrastructure.UserRepository;
import com.booking.app.common.security.SecurityUser;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class SecurityUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public SecurityUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        return userRepository
                .findByEmail(normalizeEmail(email))
                .map(this::toSecurityUser)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    private SecurityUser toSecurityUser(User user) {
        return new SecurityUser(
                user.getPublicId(),
                user.getEmail(),
                user.getPasswordHash(),
                extractAuthorities(user.getRole()),
                user.getName(),
                user.getStatus() == UserStatus.ACTIVE);
    }

    private static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private Collection<? extends GrantedAuthority> extractAuthorities(UserRole role) {
        if (role == null) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
