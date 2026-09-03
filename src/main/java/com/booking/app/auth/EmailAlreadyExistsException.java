package com.booking.app.auth;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email, Throwable cause) {
        super("User with email '%s' already exists".formatted(email), cause);
    }
}
