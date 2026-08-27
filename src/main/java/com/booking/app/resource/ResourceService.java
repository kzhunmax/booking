package com.booking.app.resource;

import com.booking.app.common.Require;
import com.booking.app.resource.internal.domain.Resource;
import com.booking.app.resource.internal.infrastructure.ResourceMapper;
import com.booking.app.resource.internal.infrastructure.ResourceRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceService {

    private static final Logger log = LoggerFactory.getLogger(ResourceService.class);
    private static final String UNIQUE_NAME_CONSTRAINT = "uk_resources_name_lower";

    private final ResourceRepository resourceRepository;

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @Transactional(readOnly = true)
    public ResourceResponse findByPublicId(UUID publicId) {
        log.debug("Fetching resource by publicId={}", publicId);
        return ResourceMapper.toResponse(requireVisibleResource(publicId));
    }

    @Transactional
    public ResourceResponse createResource(String name, String description) {
        Resource resource = new Resource(name, description);
        persist(resource);
        log.info("Resource created: publicId={}, name={}", resource.getPublicId(), name);
        return ResourceMapper.toResponse(resource);
    }

    @Transactional(readOnly = true)
    public Page<ResourceResponse> findAll(ResourceStatus status, Pageable pageable) {
        Require.notNull(status, "status cannot be null");
        Require.notNull(pageable, "pageable cannot be null");
        return resourceRepository.findByStatus(status, pageable).map(ResourceMapper::toResponse);
    }

    @Transactional
    public ResourceResponse update(UUID publicId, String name, String description) {
        Resource resource = requireVisibleResource(publicId);
        String oldName = resource.getName();
        resource.rename(name);
        resource.changeDescription(description);
        persist(resource);
        log.info("Updated resource: publicId={}, oldName={}, newName={}", publicId, oldName, name);
        return ResourceMapper.toResponse(resource);
    }

    @Transactional
    public ResourceResponse updateStatus(UUID publicId, ResourceStatus status) {
        Require.notNull(status, "status cannot be null");
        Resource resource = requireVisibleResource(publicId);
        ResourceStatus oldStatus = resource.getStatus();
        switch (status) {
            case ACTIVE -> resource.activate();
            case INACTIVE -> resource.deactivate();
            case ARCHIVED -> throw new InvalidStatusTransitionException("Resources can only be archived via DELETE");
        }
        persist(resource);
        log.info("Updated resource: publicId={}, oldStatus={}, newStatus={}", publicId, oldStatus, status);
        return ResourceMapper.toResponse(resource);
    }

    @Transactional
    public void archive(UUID publicId) {
        Resource resource = requireResource(publicId);
        resource.archive();
        log.info("Resource archived: publicId={}, name={}", publicId, resource.getName());
        persist(resource);
    }

    private Resource requireVisibleResource(UUID publicId) {
        return resourceRepository
                .findByPublicIdAndStatusNot(publicId, ResourceStatus.ARCHIVED)
                .orElseThrow(() -> new ResourceNotFoundException(publicId));
    }

    private Resource requireResource(UUID publicId) {
        return resourceRepository.findByPublicId(publicId).orElseThrow(() -> new ResourceNotFoundException(publicId));
    }

    private void persist(Resource resource) {
        try {
            resourceRepository.saveAndFlush(resource);
        } catch (DataIntegrityViolationException e) {
            if (isUniqueNameViolation(e)) {
                throw new NameAlreadyTakenException(resource.getName(), e);
            }
            throw e;
        }
    }

    private boolean isUniqueNameViolation(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();
        return message != null && message.contains(UNIQUE_NAME_CONSTRAINT);
    }
}
