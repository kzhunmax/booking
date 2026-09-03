package com.booking.app.auth;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(UUID id) {
        super("User with id %s not found".formatted(id));
    }
}
