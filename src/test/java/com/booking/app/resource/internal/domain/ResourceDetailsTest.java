package com.booking.app.resource.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ResourceDetailsTest {

    @Test
    @DisplayName("Should create ResourceDetails with valid name and description")
    void shouldCreateWithValidNameAndDescription() {
        ResourceDetails details = new ResourceDetails("Room A", "Meeting room");

        assertThat(details.name()).isEqualTo("Room A");
        assertThat(details.description()).isEqualTo("Meeting room");
    }

    @Test
    @DisplayName("Should trim name and description")
    void shouldTrimNameAndDescription() {
        ResourceDetails details = new ResourceDetails("  Room A  ", "  Meeting room  ");

        assertThat(details.name()).isEqualTo("Room A");
        assertThat(details.description()).isEqualTo("Meeting room");
    }

    @Test
    @DisplayName("Should normalize null or blank description to null")
    void shouldNormalizeBlankDescriptionToNull() {
        ResourceDetails nullDesc = new ResourceDetails("Room A", null);
        ResourceDetails blankDesc = new ResourceDetails("Room A", "   ");

        assertThat(nullDesc.description()).isNull();
        assertThat(blankDesc.description()).isNull();
    }

    @Test
    @DisplayName("Should throw exception when name is null")
    void shouldThrowExceptionWhenNameIsNull() {
        assertThatThrownBy(() -> new ResourceDetails(null, "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Name cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when name is blank")
    void shouldThrowExceptionWhenNameIsBlank() {
        assertThatThrownBy(() -> new ResourceDetails("   ", "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Name cannot be blank");
    }

    @Test
    @DisplayName("Should throw exception when name exceeds 255 characters")
    void shouldThrowExceptionWhenNameExceedsMaxLength() {
        assertThatThrownBy(() -> new ResourceDetails("a".repeat(256), "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Name cannot exceed 255 characters");
    }

    @Test
    @DisplayName("Should throw exception when description exceeds 10000 characters")
    void shouldThrowExceptionWhenDescriptionExceedsMaxLength() {
        assertThatThrownBy(() -> new ResourceDetails("Room A", "a".repeat(10001)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Description cannot exceed 10000 characters");
    }
}
