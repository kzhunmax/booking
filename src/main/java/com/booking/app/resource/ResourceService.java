package com.booking.app.resource;

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
    private final ResourceRepository resourceRepository;

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @Transactional(readOnly = true)
    public ResourceResponse findByPublicId(UUID publicId) {
        log.debug("Fetching resource by publicId={}", publicId);
        Resource resource =
                resourceRepository.findByPublicId(publicId).orElseThrow(() -> new ResourceNotFoundException(publicId));
        return ResourceMapper.toResponse(resource);
    }

    @Transactional
    public ResourceResponse createResource(String name, String description) {
        Resource resource = new Resource(name, description);
        log.debug("Creating resource with name={}", name);
        try {
            resourceRepository.saveAndFlush(resource);
            return ResourceMapper.toResponse(resource);
        } catch (DataIntegrityViolationException e) {
            throw new NameAlreadyTakenException(name, e);
        }
    }

    @Transactional(readOnly = true)
    public Page<ResourceResponse> findAll(ResourceStatus status, Pageable pageable) {
        ResourceStatus filterStatus = (status != null) ? status : ResourceStatus.ACTIVE;
        return resourceRepository.findByStatus(filterStatus, pageable).map(ResourceMapper::toResponse);
    }

    @Transactional
    public ResourceResponse update(UUID publicId, String name, String description) {
        Resource resource =
                resourceRepository.findByPublicId(publicId).orElseThrow(() -> new ResourceNotFoundException(publicId));
        resource.rename(name);
        resource.changeDescription(description);

        try {
            resourceRepository.saveAndFlush(resource);
            return ResourceMapper.toResponse(resource);
        } catch (DataIntegrityViolationException e) {
            throw new NameAlreadyTakenException(name, e);
        }
    }

    @Transactional
    public ResourceResponse updateStatus(UUID publicId, ResourceStatus status) {
        Resource resource =
                resourceRepository.findByPublicId(publicId).orElseThrow(() -> new ResourceNotFoundException(publicId));
        switch (status) {
            case ACTIVE -> resource.activate();
            case INACTIVE -> resource.deactivate();
            case ARCHIVED ->
                throw new InvalidStatusTransitionException("Archived resources cannot be changed via status update");
        }
        Resource updated = resourceRepository.save(resource);
        return ResourceMapper.toResponse(updated);
    }

    @Transactional
    public void archive(UUID publicId) {
        Resource resource =
                resourceRepository.findByPublicId(publicId).orElseThrow(() -> new ResourceNotFoundException(publicId));
        resource.archive();
        resourceRepository.save(resource);
    }
}
