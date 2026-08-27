package com.booking.app.resource;

public class ResourceCurrentlyNotAvailableException extends RuntimeException {
    public ResourceCurrentlyNotAvailableException(String message) {
        super(message);
    }
}
