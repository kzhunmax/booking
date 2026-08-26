package com.booking.app.booking.internal.domain;

public record CustomerDetails(String email, String name) {
    public CustomerDetails {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email cannot be blank");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name cannot be blank");
    }
}
