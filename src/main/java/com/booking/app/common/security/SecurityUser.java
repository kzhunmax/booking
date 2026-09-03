package com.booking.app.common.security;

import com.booking.app.auth.UserRole;
import com.booking.app.auth.UserStatus;
import java.util.Collection;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record SecurityUser(
        UUID publicId,
        String email,
        String passwordHash,
        Collection<? extends GrantedAuthority> authorities,
        String name,
        UserRole role,
        UserStatus status)
        implements UserDetails {
    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public @Nullable String getPassword() {
        return passwordHash;
    }

    @Override
    public @NonNull String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}
