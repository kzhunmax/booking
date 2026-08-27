package com.booking.app.booking.internal.domain;

import com.booking.app.common.Require;
import java.util.Locale;
import java.util.regex.Pattern;

public record CustomerDetails(String email, String name) {

    private static final int EMAIL_MAX_LENGTH = 255;
    private static final int NAME_MAX_LENGTH = 255;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public CustomerDetails {
        Require.notNull(email, "Email cannot be blank");
        Require.notNull(name, "Name cannot be blank");
        email = email.strip().toLowerCase(Locale.ROOT);
        name = name.strip();
        Require.argument(!email.isBlank(), "Email cannot be blank");
        Require.argument(!name.isBlank(), "Name cannot be blank");
        Require.argument(email.length() <= EMAIL_MAX_LENGTH, "Email cannot exceed 255 characters");
        Require.argument(name.length() <= NAME_MAX_LENGTH, "Name cannot exceed 255 characters");
        Require.argument(EMAIL_PATTERN.matcher(email).matches(), "Email is invalid");
    }
}
