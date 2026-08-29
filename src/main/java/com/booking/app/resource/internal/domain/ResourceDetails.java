package com.booking.app.resource.internal.domain;

import com.booking.app.common.Require;

public record ResourceDetails(String name, String description) {

    private static final int NAME_MAX_LENGTH = 255;
    private static final int DESCRIPTION_MAX_LENGTH = 10000;

    public ResourceDetails {
        Require.notNull(name, "Name cannot be null");
        name = name.strip();
        Require.argument(!name.isBlank(), "Name cannot be blank");
        Require.argument(name.length() <= NAME_MAX_LENGTH, "Name cannot exceed 255 characters");

        if (description != null) {
            description = description.strip();
            if (description.isEmpty()) {
                description = null;
            } else {
                Require.argument(
                        description.length() <= DESCRIPTION_MAX_LENGTH, "Description cannot exceed 10000 characters");
            }
        }
    }
}
