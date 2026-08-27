package com.booking.app.resource.internal.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.booking.app.resource.InvalidStatusTransitionException;
import com.booking.app.resource.NameAlreadyTakenException;
import com.booking.app.resource.ResourceNotFoundException;
import com.booking.app.resource.ResourceResponse;
import com.booking.app.resource.ResourceService;
import com.booking.app.resource.ResourceStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = ResourceController.class)
class ResourceControllerTest {

    @MockitoBean
    ResourceService resourceService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/resources/{publicId} - returns 200 OK with resource payload")
    void shouldReturnResourceWhenFoundByPublicId() throws Exception {
        UUID publicId = UUID.randomUUID();
        ResourceResponse response =
                new ResourceResponse(publicId, "Conference Room", "Large meeting room", ResourceStatus.ACTIVE);
        when(resourceService.findByPublicId(publicId)).thenReturn(response);

        mockMvc.perform(get("/api/resources/{publicId}", publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Conference Room"))
                .andExpect(jsonPath("$.description").value("Large meeting room"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.publicId").value(publicId.toString()));

        verify(resourceService).findByPublicId(publicId);
    }

    @Test
    @DisplayName("POST /api/resources - returns 201 Created and location header when request is valid")
    void shouldCreateResourceSuccessfully() throws Exception {
        UUID publicId = UUID.randomUUID();
        CreateResourceRequest request = new CreateResourceRequest("Conference Room", "Large meeting room");
        when(resourceService.createResource("Conference Room", "Large meeting room"))
                .thenReturn(
                        new ResourceResponse(publicId, "Conference Room", "Large meeting room", ResourceStatus.ACTIVE));

        mockMvc.perform(post("/api/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/resources/" + publicId))
                .andExpect(jsonPath("$.name").value("Conference Room"))
                .andExpect(jsonPath("$.description").value("Large meeting room"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.publicId").value(publicId.toString()));

        verify(resourceService).createResource("Conference Room", "Large meeting room");
    }

    @Test
    @DisplayName("GET /api/resources - returns 200 OK with active resources using default pageable")
    void shouldReturnActiveResourcesWithDefaultPaginationWhenStatusNotProvided() throws Exception {
        UUID publicId = UUID.randomUUID();
        ResourceResponse response =
                new ResourceResponse(publicId, "Conference Room", "Large meeting room", ResourceStatus.ACTIVE);
        Pageable pageable = PageRequest.of(0, 20);
        Page<ResourceResponse> page = new PageImpl<>(List.of(response), pageable, 1);

        when(resourceService.findAll(eq(ResourceStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Conference Room"))
                .andExpect(jsonPath("$.content[0].description").value("Large meeting room"))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.content[0].publicId").value(publicId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(resourceService).findAll(eq(ResourceStatus.ACTIVE), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/resources?status=ARCHIVED - returns 200 OK with filtered resources by status")
    void shouldReturnFilteredResourcesWhenStatusProvided() throws Exception {
        UUID publicId = UUID.randomUUID();
        ResourceResponse response =
                new ResourceResponse(publicId, "Old Room", "Archived meeting room", ResourceStatus.ARCHIVED);
        Pageable pageable = PageRequest.of(0, 20);
        Page<ResourceResponse> page = new PageImpl<>(List.of(response), pageable, 1);

        when(resourceService.findAll(eq(ResourceStatus.ARCHIVED), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/resources").param("status", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Old Room"))
                .andExpect(jsonPath("$.content[0].status").value("ARCHIVED"))
                .andExpect(jsonPath("$.content[0].publicId").value(publicId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(resourceService).findAll(eq(ResourceStatus.ARCHIVED), any(Pageable.class));
    }

    @Test
    @DisplayName("PUT /api/resources/{publicId} - returns 200 OK with updated resource")
    void shouldUpdateResourceSuccessfully() throws Exception {
        UUID publicId = UUID.randomUUID();
        UpdateResourceRequest request = new UpdateResourceRequest("Updated Name", "Updated Description");
        ResourceResponse updatedResponse =
                new ResourceResponse(publicId, "Updated Name", "Updated Description", ResourceStatus.ACTIVE);

        when(resourceService.update(publicId, request.name(), request.description()))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/api/resources/{publicId}", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.description").value("Updated Description"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.publicId").value(publicId.toString()));

        verify(resourceService).update(publicId, request.name(), request.description());
    }

    @Test
    @DisplayName("PATCH /api/resources/{publicId}/status - returns 200 OK with updated status")
    void shouldUpdateResourceStatusSuccessfully() throws Exception {
        UUID publicId = UUID.randomUUID();
        UpdateStatusRequest request = new UpdateStatusRequest(ResourceStatus.INACTIVE);
        ResourceResponse updatedResponse =
                new ResourceResponse(publicId, "Conference Room", "Large meeting room", ResourceStatus.INACTIVE);

        when(resourceService.updateStatus(publicId, ResourceStatus.INACTIVE)).thenReturn(updatedResponse);

        mockMvc.perform(patch("/api/resources/{publicId}/status", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(publicId.toString()))
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        verify(resourceService).updateStatus(publicId, ResourceStatus.INACTIVE);
    }

    @Test
    @DisplayName("DELETE /api/resources/{publicId} - returns 204 No Content when archived")
    void shouldArchiveResourceSuccessfully() throws Exception {
        UUID publicId = UUID.randomUUID();
        doNothing().when(resourceService).archive(publicId);

        mockMvc.perform(delete("/api/resources/{publicId}", publicId)).andExpect(status().isNoContent());

        verify(resourceService).archive(publicId);
    }

    @Test
    @DisplayName("GET /api/resources/{publicId} - returns 404 when resource does not exist")
    void shouldReturnNotFoundWhenResourceDoesNotExist() throws Exception {
        UUID publicId = UUID.randomUUID();
        when(resourceService.findByPublicId(publicId)).thenThrow(new ResourceNotFoundException(publicId));

        mockMvc.perform(get("/api/resources/{publicId}", publicId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Resource with id %s not found".formatted(publicId)));

        verify(resourceService).findByPublicId(publicId);
    }

    @Test
    @DisplayName("GET /api/resources/{publicId} - returns 404 when resource is archived")
    void shouldReturnNotFoundWhenResourceIsArchived() throws Exception {
        UUID publicId = UUID.randomUUID();
        when(resourceService.findByPublicId(publicId)).thenThrow(new ResourceNotFoundException(publicId));

        mockMvc.perform(get("/api/resources/{publicId}", publicId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("Resource with id %s not found".formatted(publicId)));

        verify(resourceService).findByPublicId(publicId);
    }

    @Test
    @DisplayName("GET /api/resources/{publicId} - returns 400 when publicId is not a UUID")
    void shouldReturnBadRequestWhenPublicIdIsMalformed() throws Exception {
        mockMvc.perform(get("/api/resources/{publicId}", "not-a-uuid")).andExpect(status().isBadRequest());

        verify(resourceService, never()).findByPublicId(any());
    }

    @Test
    @DisplayName("GET /api/resources?status=UNKNOWN - returns 400 when status is not a valid enum value")
    void shouldReturnBadRequestWhenStatusIsInvalid() throws Exception {
        mockMvc.perform(get("/api/resources").param("status", "UNKNOWN")).andExpect(status().isBadRequest());

        verify(resourceService, never()).findAll(any(), any());
    }

    @Test
    @DisplayName("GET /api/resources - returns 200 OK with empty page when no resources match")
    void shouldReturnEmptyPageWhenNoResourcesMatch() throws Exception {
        when(resourceService.findAll(eq(ResourceStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("POST /api/resources - returns 400 when name is blank")
    void shouldReturnBadRequestWhenCreateNameIsBlank() throws Exception {
        CreateResourceRequest request = new CreateResourceRequest("   ", "Large meeting room");

        mockMvc.perform(post("/api/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(resourceService, never()).createResource(any(), any());
    }

    @Test
    @DisplayName("POST /api/resources - returns 400 when name exceeds 255 characters")
    void shouldReturnBadRequestWhenCreateNameExceedsMaxLength() throws Exception {
        CreateResourceRequest request = new CreateResourceRequest("a".repeat(256), "Large meeting room");

        mockMvc.perform(post("/api/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(resourceService, never()).createResource(any(), any());
    }

    @Test
    @DisplayName("POST /api/resources - returns 201 Created when description is missing")
    void shouldCreateResourceWhenDescriptionIsMissing() throws Exception {
        UUID publicId = UUID.randomUUID();
        when(resourceService.createResource("Conference Room", null))
                .thenReturn(new ResourceResponse(publicId, "Conference Room", null, ResourceStatus.ACTIVE));

        mockMvc.perform(post("/api/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Conference Room\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Conference Room"))
                .andExpect(jsonPath("$.description").isEmpty())
                .andExpect(jsonPath("$.publicId").value(publicId.toString()));

        verify(resourceService).createResource("Conference Room", null);
    }

    @Test
    @DisplayName("POST /api/resources - returns 409 when name is already taken")
    void shouldReturnConflictWhenCreateNameIsAlreadyTaken() throws Exception {
        CreateResourceRequest request = new CreateResourceRequest("Conference Room", "Large meeting room");
        when(resourceService.createResource("Conference Room", "Large meeting room"))
                .thenThrow(new NameAlreadyTakenException("Conference Room", new RuntimeException("duplicate")));

        mockMvc.perform(post("/api/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource Name Taken"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Resource with name 'Conference Room' already exists"));
    }

    @Test
    @DisplayName("PUT /api/resources/{publicId} - returns 404 when resource does not exist")
    void shouldReturnNotFoundWhenUpdatingMissingResource() throws Exception {
        UUID publicId = UUID.randomUUID();
        UpdateResourceRequest request = new UpdateResourceRequest("Updated Name", "Updated Description");
        when(resourceService.update(publicId, request.name(), request.description()))
                .thenThrow(new ResourceNotFoundException(publicId));

        mockMvc.perform(put("/api/resources/{publicId}", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("PUT /api/resources/{publicId} - returns 400 when name is blank")
    void shouldReturnBadRequestWhenUpdateNameIsBlank() throws Exception {
        UUID publicId = UUID.randomUUID();
        UpdateResourceRequest request = new UpdateResourceRequest("   ", "Updated Description");

        mockMvc.perform(put("/api/resources/{publicId}", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(resourceService, never()).update(any(), any(), any());
    }

    @Test
    @DisplayName("PUT /api/resources/{publicId} - returns 409 when name is already taken")
    void shouldReturnConflictWhenUpdateNameIsAlreadyTaken() throws Exception {
        UUID publicId = UUID.randomUUID();
        UpdateResourceRequest request = new UpdateResourceRequest("Taken Name", "Updated Description");
        when(resourceService.update(publicId, request.name(), request.description()))
                .thenThrow(new NameAlreadyTakenException("Taken Name", new RuntimeException("duplicate")));

        mockMvc.perform(put("/api/resources/{publicId}", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource Name Taken"))
                .andExpect(jsonPath("$.detail").value("Resource with name 'Taken Name' already exists"));
    }

    @Test
    @DisplayName("PUT /api/resources/{publicId} - returns 404 when resource is archived")
    void shouldReturnNotFoundWhenUpdatingArchivedResource() throws Exception {
        UUID publicId = UUID.randomUUID();
        UpdateResourceRequest request = new UpdateResourceRequest("Updated Name", "Updated Description");
        when(resourceService.update(publicId, request.name(), request.description()))
                .thenThrow(new ResourceNotFoundException(publicId));

        mockMvc.perform(put("/api/resources/{publicId}", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("PUT /api/resources/{publicId} - returns 400 when domain rejects the argument")
    void shouldReturnBadRequestWhenServiceThrowsIllegalArgumentException() throws Exception {
        UUID publicId = UUID.randomUUID();
        UpdateResourceRequest request = new UpdateResourceRequest("Updated Name", "Updated Description");
        when(resourceService.update(publicId, request.name(), request.description()))
                .thenThrow(new IllegalArgumentException("Name cannot exceed 255 characters"));

        mockMvc.perform(put("/api/resources/{publicId}", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Argument"))
                .andExpect(jsonPath("$.detail").value("Name cannot exceed 255 characters"));
    }

    @Test
    @DisplayName("PATCH /api/resources/{publicId}/status - returns 404 when resource does not exist")
    void shouldReturnNotFoundWhenUpdatingStatusOfMissingResource() throws Exception {
        UUID publicId = UUID.randomUUID();
        UpdateStatusRequest request = new UpdateStatusRequest(ResourceStatus.INACTIVE);
        when(resourceService.updateStatus(publicId, ResourceStatus.INACTIVE))
                .thenThrow(new ResourceNotFoundException(publicId));

        mockMvc.perform(patch("/api/resources/{publicId}/status", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("PATCH /api/resources/{publicId}/status - returns 400 when status is missing")
    void shouldReturnBadRequestWhenPatchStatusIsMissing() throws Exception {
        UUID publicId = UUID.randomUUID();

        mockMvc.perform(patch("/api/resources/{publicId}/status", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(resourceService, never()).updateStatus(any(), any());
    }

    @Test
    @DisplayName("PATCH /api/resources/{publicId}/status - returns 422 when transition is invalid")
    void shouldReturnUnprocessableWhenStatusTransitionIsInvalid() throws Exception {
        UUID publicId = UUID.randomUUID();
        UpdateStatusRequest request = new UpdateStatusRequest(ResourceStatus.ARCHIVED);
        when(resourceService.updateStatus(publicId, ResourceStatus.ARCHIVED))
                .thenThrow(new InvalidStatusTransitionException("Resources can only be archived via DELETE"));

        mockMvc.perform(patch("/api/resources/{publicId}/status", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Resource Status Invalid Transition"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").value("Resources can only be archived via DELETE"));
    }

    @Test
    @DisplayName("DELETE /api/resources/{publicId} - returns 404 when resource does not exist")
    void shouldReturnNotFoundWhenArchivingMissingResource() throws Exception {
        UUID publicId = UUID.randomUUID();
        doThrow(new ResourceNotFoundException(publicId)).when(resourceService).archive(publicId);

        mockMvc.perform(delete("/api/resources/{publicId}", publicId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("Resource with id %s not found".formatted(publicId)));

        verify(resourceService).archive(publicId);
    }
}
