package com.booking.app.resource.internal.infrastructure;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.booking.app.TestcontainersConfiguration;
import com.booking.app.config.JpaConfig;
import com.booking.app.resource.ResourceStatus;
import com.booking.app.resource.internal.domain.Resource;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("integration")
class ResourceRepositoryTest {

    @Autowired
    private ResourceRepository resourceRepository;

    @Test
    @DisplayName("Should save and find resource by publicId")
    void shouldSaveAndFindByPublicId() {
        Resource resource = new Resource("Room A", "Description");

        resourceRepository.saveAndFlush(resource);
        Optional<Resource> found = resourceRepository.findByPublicId(resource.getPublicId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Room A");
    }

    @Test
    @DisplayName("Should find resources by status")
    void shouldFindResourcesByStatus() {
        Resource activeResource = new Resource("Active Room", "desc");
        Resource inactiveResource = new Resource("Inactive Room", "desc");
        inactiveResource.deactivate();

        resourceRepository.saveAndFlush(activeResource);
        resourceRepository.saveAndFlush(inactiveResource);

        Page<Resource> activePage = resourceRepository.findByStatus(ResourceStatus.ACTIVE, PageRequest.of(0, 10));

        assertThat(activePage.getTotalElements()).isOne();
        assertThat(activePage.getContent().getFirst().getName()).isEqualTo("Active Room");
    }

    @Test
    void shouldPersistResourceWithGeneratedFields() {
        Resource resource = new Resource("Room A", "Desc");
        Resource saved = resourceRepository.saveAndFlush(resource);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getVersion()).isNotNull();
        assertThat(saved.getAuditInfo().getCreatedAt()).isNotNull();
        assertThat(saved.getAuditInfo().getUpdatedAt()).isNotNull();
    }
}
