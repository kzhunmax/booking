package com.booking.app.resource;

import com.booking.app.resource.internal.domain.Resource;
import com.booking.app.resource.internal.exception.NameAlreadyTakenException;
import com.booking.app.resource.internal.exception.ResourceNotFoundException;
import com.booking.app.resource.internal.infrastructure.ResourceMapper;
import com.booking.app.resource.internal.infrastructure.ResourceRepository;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceService {

    private static final Logger log = LoggerFactory.getLogger(ResourceService.class);
    private final ResourceRepository resourceRepository;
    private final ResourceMapper resourceMapper;

    public ResourceService(ResourceRepository resourceRepository, ResourceMapper resourceMapper) {
        this.resourceRepository = resourceRepository;
        this.resourceMapper = resourceMapper;
    }

    private static String normalizeName(String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public ResourceResponse findByPublicId(UUID id) {
        log.debug("Fetching resource by publicId={}", id);
        Resource resource = resourceRepository.findByPublicId(id).orElseThrow(() -> new ResourceNotFoundException(id));
        return resourceMapper.toResponse(resource);
    }

    @Transactional
    public ResourceResponse createResource(ResourceRequest request) {
        String normalizedName = normalizeName(request.name());
        Resource resource = new Resource(normalizedName, request.description());
        log.debug("Creating resource with name={}", normalizedName);
        try {
            resourceRepository.saveAndFlush(resource);
            return resourceMapper.toResponse(resource);
        } catch (DataIntegrityViolationException e) {
            throw new NameAlreadyTakenException(normalizedName, e);
        }
    }
}
