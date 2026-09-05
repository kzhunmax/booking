package com.booking.app.auth.internal.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booking.app.TestcontainersConfiguration;
import com.booking.app.auth.UserRole;
import com.booking.app.auth.UserStatus;
import com.booking.app.auth.internal.domain.User;
import com.booking.app.config.JpaConfig;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("integration")
class UserRepositoryTest {

    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD_HASH = "$2a$10$somehashvalue";
    private static final String NAME = "John Doe";

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should save user and find it by publicId")
    void shouldSaveAndFindByPublicId() {
        User user = new User(EMAIL, PASSWORD_HASH, NAME, UserRole.CUSTOMER);

        userRepository.saveAndFlush(user);
        Optional<User> found = userRepository.findByPublicId(user.getPublicId());

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo(EMAIL);
        assertThat(found.get().getName()).isEqualTo(NAME);
        assertThat(found.get().getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(found.get().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should return empty Optional when publicId does not match any user")
    void shouldReturnEmptyWhenPublicIdNotFound() {
        Optional<User> found = userRepository.findByPublicId(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find user by email (case-sensitive)")
    void shouldFindUserByEmail() {
        userRepository.saveAndFlush(new User(EMAIL, PASSWORD_HASH, NAME, UserRole.CUSTOMER));

        Optional<User> found = userRepository.findByEmail(EMAIL);

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("Should return empty Optional when email does not match any user")
    void shouldReturnEmptyWhenEmailNotFound() {
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should return true from existsByEmail when email is registered")
    void shouldReturnTrueWhenEmailExists() {
        userRepository.saveAndFlush(new User(EMAIL, PASSWORD_HASH, NAME, UserRole.CUSTOMER));

        assertThat(userRepository.existsByEmail(EMAIL)).isTrue();
    }

    @Test
    @DisplayName("Should return false from existsByEmail when email is not registered")
    void shouldReturnFalseWhenEmailDoesNotExist() {
        assertThat(userRepository.existsByEmail("nobody@example.com")).isFalse();
    }

    @Test
    @DisplayName("Should populate publicId, version, createdAt and updatedAt on save")
    void shouldPopulateGeneratedFieldsOnSave() {
        User user = new User(EMAIL, PASSWORD_HASH, NAME, UserRole.CUSTOMER);

        User saved = userRepository.saveAndFlush(user);

        assertThat(saved.getPublicId()).isNotNull();
        assertThat(saved.getVersion()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should reject duplicate emails with DataIntegrityViolationException")
    void shouldRejectDuplicateEmail() {
        userRepository.saveAndFlush(new User(EMAIL, PASSWORD_HASH, NAME, UserRole.CUSTOMER));
        User duplicate = new User(EMAIL, PASSWORD_HASH, "Another Person", UserRole.CUSTOMER);

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should persist blocked status when user is blocked and re-fetched")
    void shouldPersistBlockedStatus() {
        User user = new User(EMAIL, PASSWORD_HASH, NAME, UserRole.CUSTOMER);
        userRepository.saveAndFlush(user);
        UUID publicId = user.getPublicId();

        user.block();
        userRepository.saveAndFlush(user);

        User reloaded = userRepository.findByPublicId(publicId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(UserStatus.BLOCKED);
    }

    @Test
    @DisplayName("Should allow saving multiple users with different emails")
    void shouldSaveMultipleUsersWithDifferentEmails() {
        userRepository.saveAndFlush(new User("alice@example.com", PASSWORD_HASH, "Alice", UserRole.CUSTOMER));
        userRepository.saveAndFlush(new User("bob@example.com", PASSWORD_HASH, "Bob", UserRole.ADMIN));

        assertThat(userRepository.count()).isEqualTo(2);
    }
}
