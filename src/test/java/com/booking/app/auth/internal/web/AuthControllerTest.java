package com.booking.app.auth.internal.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.booking.app.auth.AuthResponse;
import com.booking.app.auth.AuthService;
import com.booking.app.auth.EmailAlreadyExistsException;
import com.booking.app.auth.UserNotFoundException;
import com.booking.app.auth.UserResponse;
import com.booking.app.auth.UserRole;
import com.booking.app.auth.UserStatus;
import com.booking.app.common.security.SecurityUser;
import com.booking.app.config.TestSecurityConfig;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = AuthController.class)
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    private static final UUID PUBLIC_ID = UUID.randomUUID();
    private static final String EMAIL = "user@example.com";
    private static final String NAME = "John Doe";
    private static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");

    @MockitoBean
    AuthService authService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // POST /api/auth/register
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/auth/register - returns 201 Created with location header when request is valid")
    void shouldRegisterUserSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest(EMAIL, NAME, "SecureP@ss1");
        when(authService.register(EMAIL, "SecureP@ss1", NAME)).thenReturn(userResponse(PUBLIC_ID));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/users/" + PUBLIC_ID))
                .andExpect(jsonPath("$.publicId").value(PUBLIC_ID.toString()))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.name").value(NAME))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(authService).register(EMAIL, "SecureP@ss1", NAME);
    }

    @Test
    @DisplayName("POST /api/auth/register - returns 400 when email is blank")
    void shouldReturnBadRequestWhenEmailIsBlank() throws Exception {
        RegisterRequest request = new RegisterRequest("   ", NAME, "SecureP@ss1");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(), any(), any());
    }

    @Test
    @DisplayName("POST /api/auth/register - returns 400 when email is invalid format")
    void shouldReturnBadRequestWhenEmailFormatIsInvalid() throws Exception {
        RegisterRequest request = new RegisterRequest("not-an-email", NAME, "SecureP@ss1");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(), any(), any());
    }

    @Test
    @DisplayName("POST /api/auth/register - returns 400 when name is blank")
    void shouldReturnBadRequestWhenNameIsBlank() throws Exception {
        RegisterRequest request = new RegisterRequest(EMAIL, "   ", "SecureP@ss1");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(), any(), any());
    }

    @Test
    @DisplayName("POST /api/auth/register - returns 400 when password is missing uppercase")
    void shouldReturnBadRequestWhenPasswordLacksUppercase() throws Exception {
        RegisterRequest request = new RegisterRequest(EMAIL, NAME, "nouppercase1@");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(), any(), any());
    }

    @Test
    @DisplayName("POST /api/auth/register - returns 400 when password is missing lowercase")
    void shouldReturnBadRequestWhenPasswordLacksLowercase() throws Exception {
        RegisterRequest request = new RegisterRequest(EMAIL, NAME, "NOLOWERCASE1@");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(), any(), any());
    }

    @Test
    @DisplayName("POST /api/auth/register - returns 400 when password is missing a digit")
    void shouldReturnBadRequestWhenPasswordLacksDigit() throws Exception {
        RegisterRequest request = new RegisterRequest(EMAIL, NAME, "NoDigit@here");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(), any(), any());
    }

    @Test
    @DisplayName("POST /api/auth/register - returns 400 when password is missing a special character")
    void shouldReturnBadRequestWhenPasswordLacksSpecialCharacter() throws Exception {
        RegisterRequest request = new RegisterRequest(EMAIL, NAME, "NoSpecial1Char");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(), any(), any());
    }

    @Test
    @DisplayName("POST /api/auth/register - returns 400 when password is shorter than 8 characters")
    void shouldReturnBadRequestWhenPasswordIsTooShort() throws Exception {
        RegisterRequest request = new RegisterRequest(EMAIL, NAME, "Ab1@");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(), any(), any());
    }

    @Test
    @DisplayName("POST /api/auth/register - returns 409 when email is already registered")
    void shouldReturnConflictWhenEmailIsAlreadyRegistered() throws Exception {
        RegisterRequest request = new RegisterRequest(EMAIL, NAME, "SecureP@ss1");
        when(authService.register(anyString(), anyString(), anyString()))
                .thenThrow(new EmailAlreadyExistsException(EMAIL, new RuntimeException("duplicate")));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Email Already Exists"))
                .andExpect(jsonPath("$.status").value(409));
    }

    // -------------------------------------------------------------------------
    // POST /api/auth/login
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/auth/login - returns 200 OK with JWT token for valid credentials")
    void shouldLoginAndReturnToken() throws Exception {
        LoginRequest request = new LoginRequest(EMAIL, "SecureP@ss1");
        when(authService.login(EMAIL, "SecureP@ss1")).thenReturn(new AuthResponse("jwt-token-value"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-value"));

        verify(authService).login(EMAIL, "SecureP@ss1");
    }

    @Test
    @DisplayName("POST /api/auth/login - returns 400 when email is blank")
    void shouldReturnBadRequestWhenLoginEmailIsBlank() throws Exception {
        LoginRequest request = new LoginRequest("   ", "SecureP@ss1");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any(), any());
    }

    @Test
    @DisplayName("POST /api/auth/login - returns 400 when password is blank")
    void shouldReturnBadRequestWhenLoginPasswordIsBlank() throws Exception {
        LoginRequest request = new LoginRequest(EMAIL, "   ");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any(), any());
    }

    @Test
    @DisplayName("POST /api/auth/login - returns 401 when credentials are wrong")
    void shouldReturnUnauthorizedWhenCredentialsAreInvalid() throws Exception {
        LoginRequest request = new LoginRequest(EMAIL, "WrongP@ss1");
        when(authService.login(anyString(), anyString())).thenThrow(new BadCredentialsException("bad creds"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Bad Credentials"));
    }

    // -------------------------------------------------------------------------
    // GET /api/auth/me
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/auth/me - returns 200 OK with current user profile when authenticated")
    @WithMockUser(
            username = "user@example.com",
            roles = {"CUSTOMER"})
    void shouldReturnCurrentUserProfile() throws Exception {
        SecurityUser principal = new SecurityUser(
                PUBLIC_ID, EMAIL, "$2a$hash", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")), NAME, true);
        when(authService.getProfile(any())).thenReturn(userResponse(PUBLIC_ID));

        mockMvc.perform(
                        get("/api/auth/me")
                                .with(org.springframework.security.test.web.servlet.request
                                        .SecurityMockMvcRequestPostProcessors.user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(PUBLIC_ID.toString()))
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    @DisplayName("GET /api/auth/me - returns 404 when authenticated user is not found in repository")
    @WithMockUser(
            username = "user@example.com",
            roles = {"CUSTOMER"})
    void shouldReturnNotFoundWhenUserProfileIsMissing() throws Exception {
        SecurityUser principal = new SecurityUser(
                PUBLIC_ID, EMAIL, "$2a$hash", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")), NAME, true);
        when(authService.getProfile(any())).thenThrow(new UserNotFoundException(PUBLIC_ID));

        mockMvc.perform(
                        get("/api/auth/me")
                                .with(org.springframework.security.test.web.servlet.request
                                        .SecurityMockMvcRequestPostProcessors.user(principal)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("User Not Found"));
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private static UserResponse userResponse(UUID publicId) {
        return new UserResponse(publicId, EMAIL, NAME, UserRole.CUSTOMER, UserStatus.ACTIVE, NOW, NOW);
    }
}
