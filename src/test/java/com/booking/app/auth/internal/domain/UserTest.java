package com.booking.app.auth.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booking.app.auth.UserRole;
import com.booking.app.auth.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD_HASH = "$2a$10$somehashvalue";
    private static final String NAME = "John Doe";
    private static final UserRole ROLE = UserRole.CUSTOMER;

    private User createValidUser() {
        return new User(EMAIL, PASSWORD_HASH, NAME, ROLE);
    }

    @Nested
    @DisplayName("User Creation")
    class Creation {

        @Test
        @DisplayName("Should create user with all fields populated and status ACTIVE")
        void shouldCreateUserWithValidFields() {
            User user = createValidUser();

            assertThat(user.getPublicId()).isNotNull();
            assertThat(user.getEmail()).isEqualTo(EMAIL);
            assertThat(user.getPasswordHash()).isEqualTo(PASSWORD_HASH);
            assertThat(user.getName()).isEqualTo(NAME);
            assertThat(user.getRole()).isEqualTo(UserRole.CUSTOMER);
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should generate unique publicId for each user")
        void shouldGenerateUniquePublicId() {
            User first = createValidUser();
            User second = createValidUser();

            assertThat(first.getPublicId()).isNotEqualTo(second.getPublicId());
        }

        @Test
        @DisplayName("Should throw when email is null")
        void shouldThrowWhenEmailIsNull() {
            assertThatThrownBy(() -> new User(null, PASSWORD_HASH, NAME, ROLE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("email cannot be null");
        }

        @Test
        @DisplayName("Should throw when email is blank")
        void shouldThrowWhenEmailIsBlank() {
            assertThatThrownBy(() -> new User("   ", PASSWORD_HASH, NAME, ROLE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("email cannot be empty");
        }

        @Test
        @DisplayName("Should throw when passwordHash is null")
        void shouldThrowWhenPasswordHashIsNull() {
            assertThatThrownBy(() -> new User(EMAIL, null, NAME, ROLE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("passwordHash cannot be null");
        }

        @Test
        @DisplayName("Should throw when passwordHash is blank")
        void shouldThrowWhenPasswordHashIsBlank() {
            assertThatThrownBy(() -> new User(EMAIL, "   ", NAME, ROLE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("passwordHash cannot be empty");
        }

        @Test
        @DisplayName("Should throw when name is null")
        void shouldThrowWhenNameIsNull() {
            assertThatThrownBy(() -> new User(EMAIL, PASSWORD_HASH, null, ROLE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("name cannot be null");
        }

        @Test
        @DisplayName("Should throw when name is blank")
        void shouldThrowWhenNameIsBlank() {
            assertThatThrownBy(() -> new User(EMAIL, PASSWORD_HASH, "   ", ROLE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("name cannot be empty");
        }

        @Test
        @DisplayName("Should throw when role is null")
        void shouldThrowWhenRoleIsNull() {
            assertThatThrownBy(() -> new User(EMAIL, PASSWORD_HASH, NAME, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("role cannot be null");
        }

        @Test
        @DisplayName("Should create ADMIN user when role is ADMIN")
        void shouldCreateAdminUser() {
            User user = new User(EMAIL, PASSWORD_HASH, NAME, UserRole.ADMIN);

            assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("Block and Unblock")
    class BlockUnblock {

        @Test
        @DisplayName("Should change status to BLOCKED when blocking an ACTIVE user")
        void shouldBlockActiveUser() {
            User user = createValidUser();

            user.block();

            assertThat(user.getStatus()).isEqualTo(UserStatus.BLOCKED);
        }

        @Test
        @DisplayName("Should be idempotent when blocking an already BLOCKED user")
        void shouldBeIdempotentWhenBlockingAlreadyBlockedUser() {
            User user = createValidUser();
            user.block();

            user.block();

            assertThat(user.getStatus()).isEqualTo(UserStatus.BLOCKED);
        }

        @Test
        @DisplayName("Should change status to ACTIVE when unblocking a BLOCKED user")
        void shouldUnblockBlockedUser() {
            User user = createValidUser();
            user.block();

            user.unblock();

            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should be idempotent when unblocking an already ACTIVE user")
        void shouldBeIdempotentWhenUnblockingAlreadyActiveUser() {
            User user = createValidUser();

            user.unblock();

            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should allow block → unblock → block cycle")
        void shouldSupportRepeatedBlockUnblockCycle() {
            User user = createValidUser();

            user.block();
            assertThat(user.getStatus()).isEqualTo(UserStatus.BLOCKED);

            user.unblock();
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);

            user.block();
            assertThat(user.getStatus()).isEqualTo(UserStatus.BLOCKED);
        }
    }
}
