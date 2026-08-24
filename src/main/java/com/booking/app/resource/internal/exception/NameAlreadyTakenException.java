package com.booking.app.resource.internal.exception;

public class NameAlreadyTakenException extends RuntimeException {
    public NameAlreadyTakenException(String name, Throwable cause) {
        super("Resource with name '%s' already exists".formatted(name), cause);
    }
}
