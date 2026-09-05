package com.booking.app.common.security;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
        boolean enabled)
        implements UserDetails {

    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities != null ? authorities : Collections.emptyList();
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
        return enabled;
    }

    public boolean hasRole(String role) {
        if (role == null || authorities == null) {
            return false;
        }
        String target = ROLE_PREFIX + role.toUpperCase(Locale.ROOT);
        return authorities.stream().anyMatch(a -> target.equalsIgnoreCase(a.getAuthority()));
    }

    public boolean hasAuthority(String authority) {
        if (authority == null || authorities == null) {
            return false;
        }
        return authorities.stream().anyMatch(a -> authority.equalsIgnoreCase(a.getAuthority()));
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public Set<String> getRoles() {
        if (authorities == null) {
            return Collections.emptySet();
        }
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .filter(a -> a.startsWith(ROLE_PREFIX))
                .map(a -> a.substring(ROLE_PREFIX.length()))
                .collect(Collectors.toUnmodifiableSet());
    }

    public String role() {
        return getRoles().stream().findFirst().orElse("");
    }
}
