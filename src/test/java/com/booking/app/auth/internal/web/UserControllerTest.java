package com.booking.app.auth.internal.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.booking.app.auth.AuthService;
import com.booking.app.auth.UserNotFoundException;
import com.booking.app.auth.UserResponse;
import com.booking.app.auth.UserRole;
import com.booking.app.auth.UserStatus;
import com.booking.app.config.TestSecurityConfig;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = UserController.class)
@Import(TestSecurityConfig.class)
class UserControllerTest {

    private static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");

    @MockitoBean
    AuthService authService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // GET /api/users
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/users - returns 200 OK with paginated users for ADMIN")
    @WithMockUser(roles = "ADMIN")
    void shouldReturnPagedUsersForAdmin() throws Exception {
        UUID publicId = UUID.randomUUID();
        UserResponse user = userResponse(publicId, UserStatus.ACTIVE);
        Page<UserResponse> page = new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1);
        when(authService.getAllUsers(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].publicId").value(publicId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(authService).getAllUsers(any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/users - returns 403 Forbidden for CUSTOMER role")
    @WithMockUser(roles = "CUSTOMER")
    void shouldReturnForbiddenForCustomer() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isForbidden());

        verify(authService, never()).getAllUsers(any());
    }

    @Test
    @DisplayName("GET /api/users - returns 401 Unauthorized when not authenticated")
    void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());

        verify(authService, never()).getAllUsers(any());
    }

    @Test
    @DisplayName("GET /api/users - returns 200 OK with empty page when no users exist")
    @WithMockUser(roles = "ADMIN")
    void shouldReturnEmptyPageWhenNoUsersExist() throws Exception {
        when(authService.getAllUsers(any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/users/{publicId}/status
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PATCH /api/users/{publicId}/status - returns 200 OK with updated status for ADMIN")
    @WithMockUser(roles = "ADMIN")
    void shouldUpdateUserStatusForAdmin() throws Exception {
        UUID publicId = UUID.randomUUID();
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.BLOCKED);
        when(authService.updateStatus(publicId, UserStatus.BLOCKED))
                .thenReturn(userResponse(publicId, UserStatus.BLOCKED));

        mockMvc.perform(patch("/api/users/{publicId}/status", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(publicId.toString()))
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        verify(authService).updateStatus(eq(publicId), eq(UserStatus.BLOCKED));
    }

    @Test
    @DisplayName("PATCH /api/users/{publicId}/status - returns 200 OK when unblocking user")
    @WithMockUser(roles = "ADMIN")
    void shouldUnblockUserForAdmin() throws Exception {
        UUID publicId = UUID.randomUUID();
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.ACTIVE);
        when(authService.updateStatus(publicId, UserStatus.ACTIVE))
                .thenReturn(userResponse(publicId, UserStatus.ACTIVE));

        mockMvc.perform(patch("/api/users/{publicId}/status", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("PATCH /api/users/{publicId}/status - returns 400 when status is missing from request body")
    @WithMockUser(roles = "ADMIN")
    void shouldReturnBadRequestWhenStatusIsMissing() throws Exception {
        UUID publicId = UUID.randomUUID();

        mockMvc.perform(patch("/api/users/{publicId}/status", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(authService, never()).updateStatus(any(), any());
    }

    @Test
    @DisplayName("PATCH /api/users/{publicId}/status - returns 400 when publicId is not a valid UUID")
    @WithMockUser(roles = "ADMIN")
    void shouldReturnBadRequestWhenPublicIdIsMalformed() throws Exception {
        mockMvc.perform(patch("/api/users/{publicId}/status", "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserStatusRequest(UserStatus.BLOCKED))))
                .andExpect(status().isBadRequest());

        verify(authService, never()).updateStatus(any(), any());
    }

    @Test
    @DisplayName("PATCH /api/users/{publicId}/status - returns 404 when user does not exist")
    @WithMockUser(roles = "ADMIN")
    void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        UUID publicId = UUID.randomUUID();
        when(authService.updateStatus(publicId, UserStatus.BLOCKED)).thenThrow(new UserNotFoundException(publicId));

        mockMvc.perform(patch("/api/users/{publicId}/status", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserStatusRequest(UserStatus.BLOCKED))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("User Not Found"))
                .andExpect(jsonPath("$.detail").value("User with id %s not found".formatted(publicId)));
    }

    @Test
    @DisplayName("PATCH /api/users/{publicId}/status - returns 403 Forbidden for CUSTOMER role")
    @WithMockUser(roles = "CUSTOMER")
    void shouldReturnForbiddenWhenCustomerTriesToUpdateStatus() throws Exception {
        UUID publicId = UUID.randomUUID();

        mockMvc.perform(patch("/api/users/{publicId}/status", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserStatusRequest(UserStatus.BLOCKED))))
                .andExpect(status().isForbidden());

        verify(authService, never()).updateStatus(any(), any());
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private static UserResponse userResponse(UUID publicId, UserStatus status) {
        return new UserResponse(publicId, "user@example.com", "John Doe", UserRole.CUSTOMER, status, NOW, NOW);
    }
}
