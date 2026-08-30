package com.booking.app.resource.internal.infrastructure;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.booking.app.TestcontainersConfiguration;
import com.booking.app.config.JpaConfig;
import com.booking.app.resource.ResourceStatus;
import com.booking.app.resource.internal.domain.Resource;
import com.booking.app.resource.internal.domain.ResourceDetails;
import com.booking.app.resource.internal.domain.ResourcePricing;
import java.math.BigDecimal;
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

    private static final BigDecimal DEFAULT_PRICE = BigDecimal.valueOf(100.0);
    private static final String DEFAULT_CURRENCY = "USD";

    @Autowired
    private ResourceRepository resourceRepository;

    private static Resource resource(String name, String description) {
        return new Resource(
                new ResourceDetails(name, description), new ResourcePricing(DEFAULT_PRICE, DEFAULT_CURRENCY));
    }

    @Test
    @DisplayName("Should save and find resource by publicId")
    void shouldSaveAndFindByPublicId() {
        Resource res = resource("Room A", "Description");

        resourceRepository.saveAndFlush(res);
        Optional<Resource> found = resourceRepository.findByPublicId(res.getPublicId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Room A");
        assertThat(found.get().getPricePerHour()).isEqualByComparingTo(DEFAULT_PRICE);
        assertThat(found.get().getCurrency()).isEqualTo("USD");
    }

    @Test
    @DisplayName("Should find resources by status")
    void shouldFindResourcesByStatus() {
        Resource activeResource = resource("Active Room", "desc");
        Resource inactiveResource = resource("Inactive Room", "desc");
        inactiveResource.deactivate();

        resourceRepository.saveAndFlush(activeResource);
        resourceRepository.saveAndFlush(inactiveResource);

        Page<Resource> activePage = resourceRepository.findByStatus(ResourceStatus.ACTIVE, PageRequest.of(0, 10));

        assertThat(activePage.getTotalElements()).isOne();
        assertThat(activePage.getContent().getFirst().getName()).isEqualTo("Active Room");
    }

    @Test
    void shouldPersistResourceWithGeneratedFields() {
        Resource res = resource("Room A", "Desc");
        Resource saved = resourceRepository.saveAndFlush(res);
        assertThat(saved.getVersion()).isNotNull();
        assertThat(saved.getAuditInfo().getCreatedAt()).isNotNull();
        assertThat(saved.getAuditInfo().getUpdatedAt()).isNotNull();
    }
}
