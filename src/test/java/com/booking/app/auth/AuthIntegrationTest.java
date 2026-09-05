package com.booking.app.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.booking.app.TestcontainersConfiguration;
import com.booking.app.auth.internal.web.LoginRequest;
import com.booking.app.auth.internal.web.RegisterRequest;
import com.booking.app.auth.internal.web.UpdateUserStatusRequest;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
@Tag("integration")
class AuthIntegrationTest {

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Autowired
    private RestTestClient restTestClient;

    // -------------------------------------------------------------------------
    // Register
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/auth/register - returns 201 Created with UserResponse and Location header")
    void shouldRegisterNewUserSuccessfully() {
        RegisterRequest request = registerRequest(uniqueEmail(), "SecureP@ss1");

        restTestClient
                .post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectHeader()
                .exists(HttpHeaders.LOCATION)
                .expectBody(UserResponse.class)
                .value(response -> {
                    assertThat(response.publicId()).isNotNull();
                    assertThat(response.email()).isEqualTo(request.email());
                    assertThat(response.name()).isEqualTo(request.name());
                    assertThat(response.role()).isEqualTo(UserRole.CUSTOMER);
                    assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
                });
    }

    @Test
    @DisplayName("POST /api/auth/register - returns 409 when email is already registered")
    void shouldReturnConflictWhenEmailIsAlreadyRegistered() {
        String email = uniqueEmail();
        RegisterRequest request = registerRequest(email, "SecureP@ss1");

        restTestClient
                .post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus()
                .isCreated();

        restTestClient
                .post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.title")
                .isEqualTo("Email Already Exists");
    }

    @Test
    @DisplayName("POST /api/auth/register - returns 400 when password fails complexity rules")
    void shouldReturnBadRequestWhenPasswordIsWeak() {
        RegisterRequest request = registerRequest(uniqueEmail(), "weakpassword");

        restTestClient
                .post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/auth/login - returns 200 OK with JWT token for valid credentials")
    void shouldLoginAndReturnJwtToken() {
        String email = uniqueEmail();
        String password = "SecureP@ss1";
        register(email, password);

        AuthResponse response = restTestClient
                .post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(email, password))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.token()).isNotBlank();
    }

    @Test
    @DisplayName("POST /api/auth/login - returns 401 when password is wrong")
    void shouldReturnUnauthorizedWhenPasswordIsWrong() {
        String email = uniqueEmail();
        register(email, "SecureP@ss1");

        restTestClient
                .post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(email, "WrongP@ss9"))
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.title")
                .isEqualTo("Bad Credentials");
    }

    @Test
    @DisplayName("POST /api/auth/login - normalises email (uppercase input) to lowercase")
    void shouldLoginWithUppercaseEmail() {
        String email = uniqueEmail();
        register(email, "SecureP@ss1");

        restTestClient
                .post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(email.toUpperCase(java.util.Locale.ROOT), "SecureP@ss1"))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.token")
                .isNotEmpty();
    }

    // -------------------------------------------------------------------------
    // GET /api/auth/me
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/auth/me - returns 200 OK with current user's profile using valid JWT")
    void shouldReturnCurrentUserProfileWithValidJwt() {
        String email = uniqueEmail();
        String token = registerAndLogin(email, "SecureP@ss1");

        restTestClient
                .get()
                .uri("/api/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UserResponse.class)
                .value(response -> {
                    assertThat(response.email()).isEqualTo(email);
                    assertThat(response.role()).isEqualTo(UserRole.CUSTOMER);
                    assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
                });
    }

    @Test
    @DisplayName("GET /api/auth/me - returns 401 when no JWT is provided")
    void shouldReturnUnauthorizedWhenNoJwtProvided() {
        restTestClient.get().uri("/api/auth/me").exchange().expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("GET /api/auth/me - returns 401 when JWT is invalid")
    void shouldReturnUnauthorizedWhenJwtIsInvalid() {
        restTestClient
                .get()
                .uri("/api/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token")
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    // -------------------------------------------------------------------------
    // GET /api/users (admin-only)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/users - returns 200 OK with user page for ADMIN")
    void shouldReturnUserPageForAdmin() {
        String adminToken = loginAdmin();

        restTestClient
                .get()
                .uri("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.content")
                .isArray()
                .jsonPath("$.totalElements")
                .isNumber();
    }

    @Test
    @DisplayName("GET /api/users - returns 403 Forbidden for CUSTOMER role")
    void shouldReturnForbiddenForCustomerOnGetUsers() {
        String customerToken = registerAndLogin(uniqueEmail(), "SecureP@ss1");

        restTestClient
                .get()
                .uri("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    @DisplayName("GET /api/users - returns 401 when not authenticated")
    void shouldReturnUnauthorizedOnGetUsersWithoutToken() {
        restTestClient.get().uri("/api/users").exchange().expectStatus().isUnauthorized();
    }

    // -------------------------------------------------------------------------
    // PATCH /api/users/{publicId}/status (admin-only)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PATCH /api/users/{publicId}/status - admin can block a customer")
    void shouldAllowAdminToBlockCustomer() {
        String email = uniqueEmail();
        UserResponse customer = register(email, "SecureP@ss1");
        String adminToken = loginAdmin();

        restTestClient
                .patch()
                .uri("/api/users/{publicId}/status", customer.publicId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateUserStatusRequest(UserStatus.BLOCKED))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UserResponse.class)
                .value(response -> assertThat(response.status()).isEqualTo(UserStatus.BLOCKED));
    }

    @Test
    @DisplayName("PATCH /api/users/{publicId}/status - admin can unblock a blocked customer")
    void shouldAllowAdminToUnblockCustomer() {
        String email = uniqueEmail();
        UserResponse customer = register(email, "SecureP@ss1");
        String adminToken = loginAdmin();

        // Block first
        restTestClient
                .patch()
                .uri("/api/users/{publicId}/status", customer.publicId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateUserStatusRequest(UserStatus.BLOCKED))
                .exchange()
                .expectStatus()
                .isOk();

        // Then unblock
        restTestClient
                .patch()
                .uri("/api/users/{publicId}/status", customer.publicId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateUserStatusRequest(UserStatus.ACTIVE))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UserResponse.class)
                .value(response -> assertThat(response.status()).isEqualTo(UserStatus.ACTIVE));
    }

    @Test
    @DisplayName("PATCH /api/users/{publicId}/status - returns 403 Forbidden for CUSTOMER role")
    void shouldReturnForbiddenWhenCustomerTriesToBlockUser() {
        String email = uniqueEmail();
        UserResponse target = register(email, "SecureP@ss1");
        String anotherCustomerToken = registerAndLogin(uniqueEmail(), "SecureP@ss1");

        restTestClient
                .patch()
                .uri("/api/users/{publicId}/status", target.publicId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + anotherCustomerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateUserStatusRequest(UserStatus.BLOCKED))
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    @DisplayName("PATCH /api/users/{publicId}/status - returns 404 when user does not exist")
    void shouldReturnNotFoundWhenBlockingNonExistentUser() {
        String adminToken = loginAdmin();
        UUID nonExistentId = UUID.randomUUID();

        restTestClient
                .patch()
                .uri("/api/users/{publicId}/status", nonExistentId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateUserStatusRequest(UserStatus.BLOCKED))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.title")
                .isEqualTo("User Not Found");
    }

    @Test
    @DisplayName("Blocked user cannot login — Spring Security rejects disabled principal")
    void shouldRejectLoginForBlockedUser() {
        String email = uniqueEmail();
        String password = "SecureP@ss1";
        UserResponse customer = register(email, password);
        String adminToken = loginAdmin();

        // Block the user
        restTestClient
                .patch()
                .uri("/api/users/{publicId}/status", customer.publicId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateUserStatusRequest(UserStatus.BLOCKED))
                .exchange()
                .expectStatus()
                .isOk();

        // Attempt login as blocked user — should get 401
        restTestClient
                .post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(email, password))
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private UserResponse register(String email, String password) {
        UserResponse body = restTestClient
                .post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(registerRequest(email, password))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(UserResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(body).isNotNull();
        return body;
    }

    private String registerAndLogin(String email, String password) {
        register(email, password);
        return login(email, password);
    }

    private String login(String email, String password) {
        AuthResponse body = restTestClient
                .post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(email, password))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(body).isNotNull();
        return body.token();
    }

    private String loginAdmin() {
        return login(adminEmail, adminPassword);
    }

    private static RegisterRequest registerRequest(String email, String password) {
        return new RegisterRequest(email, "John Doe", password);
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
