package com.booking.app.resource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booking.app.resource.internal.domain.Resource;
import com.booking.app.resource.internal.infrastructure.ResourceRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @InjectMocks
    private ResourceService resourceService;

    private Resource testResource;

    @BeforeEach
    void setUp() {
        testResource = new Resource("Conference Room", "description");
    }

    @Test
    @DisplayName("Should find resource by publicId")
    void shouldFindResourceByPublicId() {
        UUID publicId = testResource.getPublicId();
        when(resourceRepository.findByPublicId(publicId)).thenReturn(Optional.of(testResource));

        ResourceResponse response = resourceService.findByPublicId(publicId);

        assertThat(response.publicId()).isEqualTo(publicId);
        assertThat(response.name()).isEqualTo("Conference Room");
        verify(resourceRepository).findByPublicId(publicId);
    }

    @Test
    @DisplayName("Should findByPublicId throw ResourceNotFoundException when resource does not exist")
    void shouldFindByPublicIdThrowResourceNotFoundExceptionWhenResourceDoesNotExist() {
        UUID publicId = testResource.getPublicId();
        when(resourceRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.findByPublicId(publicId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should create resource when name is unique")
    void shouldCreateResourceWhenNameIsUnique() {
        String name = "New Room";
        String description = "New description";
        when(resourceRepository.saveAndFlush(any(Resource.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResourceResponse response = resourceService.createResource(name, description);

        assertThat(response.name()).isEqualTo(name);
        verify(resourceRepository).saveAndFlush(any(Resource.class));
    }

    @Test
    @DisplayName("Should throw NameAlreadyTakenException when name already exists")
    void shouldThrowNameAlreadyTakenExceptionWhenNameAlreadyExists() {
        Throwable cause = new RuntimeException("uk_resources_name_lower");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Duplicate entry for 'name'", cause);
        when(resourceRepository.saveAndFlush(any(Resource.class))).thenThrow(ex);

        assertThatThrownBy(() -> resourceService.createResource("New Room", "description"))
                .isInstanceOf(NameAlreadyTakenException.class);
    }

    @Test
    @DisplayName("Should rethrow DataIntegrityViolationException for unexpected constraint")
    void shouldRethrowDataIntegrityViolationExceptionForUnexpectedConstraint() {
        Throwable cause = new RuntimeException("some cause");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("some error", cause);
        when(resourceRepository.saveAndFlush(any(Resource.class))).thenThrow(ex);

        assertThatThrownBy(() -> resourceService.createResource("New Room", "description"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should return pageable resources")
    void shouldReturnPageableResources() {
        ResourceStatus status = ResourceStatus.ACTIVE;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Resource> page = new PageImpl<>(List.of(testResource), pageable, 1);
        when(resourceRepository.findByStatus(status, pageable)).thenReturn(page);

        Page<ResourceResponse> result = resourceService.findAll(status, pageable);

        assertThat(result.getTotalElements()).isOne();
        assertThat(result.getContent().getFirst().name()).isEqualTo("Conference Room");
        verify(resourceRepository).findByStatus(status, pageable);
    }

    @Test
    @DisplayName("Should update throw ResourceNotFoundException when resource does not exist")
    void shouldUpdateThrowResourceNotFoundExceptionWhenResourceDoesNotExist() {
        UUID publicId = testResource.getPublicId();
        when(resourceRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.update(publicId, "Room A", "description"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should update resource name and description")
    void shouldUpdateResourceNameAndDescription() {
        UUID publicId = testResource.getPublicId();
        String newName = "New Room";
        String newDescription = "New description";
        when(resourceRepository.findByPublicId(publicId)).thenReturn(Optional.of(testResource));
        when(resourceRepository.saveAndFlush(any(Resource.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResourceResponse response = resourceService.update(publicId, newName, newDescription);

        assertThat(response.name()).isEqualTo(newName);
        assertThat(response.description()).isEqualTo(newDescription);
        verify(resourceRepository).findByPublicId(publicId);
        verify(resourceRepository).saveAndFlush(any(Resource.class));
    }

    @Test
    @DisplayName("Should update status throw ResourceNotFoundException when resource does not exist")
    void shouldUpdateStatusThrowResourceNotFoundExceptionWhenResourceDoesNotExist() {
        UUID publicId = testResource.getPublicId();
        when(resourceRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.updateStatus(publicId, ResourceStatus.INACTIVE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @ParameterizedTest
    @EnumSource(
            value = ResourceStatus.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = {"ARCHIVED"})
    @DisplayName("Should update resource status successfully")
    void shouldUpdateResourceStatusSuccessfully(ResourceStatus newStatus) {
        UUID publicId = testResource.getPublicId();
        when(resourceRepository.findByPublicId(publicId)).thenReturn(Optional.of(testResource));
        when(resourceRepository.save(any(Resource.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResourceResponse response = resourceService.updateStatus(publicId, newStatus);

        assertThat(response.status()).isEqualTo(newStatus);
        verify(resourceRepository).findByPublicId(publicId);
        verify(resourceRepository).save(any(Resource.class));
    }

    @Test
    @DisplayName("Should throw InvalidStatusTransitionException when update with status ARCHIVED")
    void shouldThrowInvalidStatusTransitionExceptionWhenUpdateWithStatusArchived() {
        UUID publicId = testResource.getPublicId();
        when(resourceRepository.findByPublicId(publicId)).thenReturn(Optional.of(testResource));

        assertThatThrownBy(() -> resourceService.updateStatus(publicId, ResourceStatus.ARCHIVED))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessage("Resources can only be archived via DELETE");
        verify(resourceRepository).findByPublicId(publicId);
        verify(resourceRepository, never()).save(any(Resource.class));
    }

    @ParameterizedTest
    @EnumSource(
            value = ResourceStatus.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = {"ARCHIVED"})
    @DisplayName("Should throw InvalidStatusTransitionException when update status already ARCHIVED")
    void shouldThrowInvalidStatusTransitionExceptionWhenUpdateStatusAlreadyArchived(ResourceStatus newStatus) {
        UUID publicId = testResource.getPublicId();
        testResource.archive();
        when(resourceRepository.findByPublicId(publicId)).thenReturn(Optional.of(testResource));

        assertThatThrownBy(() -> resourceService.updateStatus(publicId, newStatus))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessage("Archived resources cannot be changed");
        verify(resourceRepository).findByPublicId(publicId);
        verify(resourceRepository, never()).save(any(Resource.class));
    }

    @Test
    @DisplayName("Should archive throw ResourceNotFoundException when resource does not exist")
    void shouldArchiveThrowResourceNotFoundExceptionWhenResourceDoesNotExist() {
        UUID publicId = testResource.getPublicId();
        when(resourceRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.archive(publicId)).isInstanceOf(ResourceNotFoundException.class);
        verify(resourceRepository, never()).save(any(Resource.class));
    }

    @Test
    @DisplayName("Should archive resource")
    void shouldArchiveResource() {
        UUID publicId = testResource.getPublicId();
        when(resourceRepository.findByPublicId(publicId)).thenReturn(Optional.of(testResource));
        when(resourceRepository.save(any(Resource.class))).thenAnswer(invocation -> invocation.getArgument(0));

        resourceService.archive(publicId);

        assertThat(testResource.getStatus()).isEqualTo(ResourceStatus.ARCHIVED);
        verify(resourceRepository).save(testResource);
    }
}
