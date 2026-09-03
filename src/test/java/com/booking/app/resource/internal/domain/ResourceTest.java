package com.booking.app.resource.internal.domain;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.booking.app.resource.InvalidStatusTransitionException;
import com.booking.app.resource.ResourceStatus;
import java.math.BigDecimal;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ResourceTest {

    private ResourceDetails defaultDetails;
    private ResourcePricing defaultPricing;

    @BeforeEach
    void setUp() {
        defaultDetails = new ResourceDetails("Conference Room A", "Large Meeting Room");
        defaultPricing = new ResourcePricing(BigDecimal.valueOf(100), "USD");
    }

    private Resource createValidResource() {
        return new Resource(defaultDetails, defaultPricing);
    }

    @Nested
    @DisplayName("Resource Creation")
    class Creation {

        @Test
        @DisplayName("Should create resource with valid details and pricing")
        void shouldCreateResourceWithValidFields() {
            Resource resource = createValidResource();

            assertThat(resource.getName()).isEqualTo("Conference Room A");
            assertThat(resource.getDescription()).isEqualTo("Large Meeting Room");
            assertThat(resource.getStatus()).isEqualTo(ResourceStatus.ACTIVE);
            assertThat(resource.getPricePerHour()).isEqualTo(BigDecimal.valueOf(100));
            assertThat(resource.getCurrency()).isEqualTo("USD");
            assertThat(resource.getPublicId()).isNotNull();
            assertThat(resource.getCreatedAt()).isNull();
            assertThat(resource.getUpdatedAt()).isNull();

            // Hibernate generates
            assertThat(resource.getVersion()).isNull();
            assertThat(resource.getCreatedAt()).isNull();
            assertThat(resource.getUpdatedAt()).isNull();
        }

        @Test
        @DisplayName("Should throw exception when details is null")
        void shouldThrowExceptionWhenDetailsIsNull() {
            assertThatThrownBy(() -> new Resource(null, defaultPricing))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("details cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when pricing is null")
        void shouldThrowExceptionWhenPricingIsNull() {
            assertThatThrownBy(() -> new Resource(defaultDetails, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("pricing cannot be null");
        }
    }

    @Nested
    @DisplayName("Status Transitions")
    class StatusTransitions {

        @Test
        @DisplayName("Should activate inactive resource")
        void shouldActivateInactiveResource() {
            Resource resource = createValidResource();
            resource.deactivate();

            resource.activate();

            assertThat(resource.getStatus()).isEqualTo(ResourceStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should not activate archived resource")
        void shouldNotActivateArchivedResource() {
            Resource resource = createValidResource();
            resource.archive();

            assertThatThrownBy(resource::activate)
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessage("Archived resources cannot be changed");
        }

        @Test
        @DisplayName("Should archive active resource")
        void shouldArchiveActiveResource() {
            Resource resource = createValidResource();

            resource.archive();

            assertThat(resource.getStatus()).isEqualTo(ResourceStatus.ARCHIVED);
        }

        @Test
        @DisplayName("Should not change archived resource twice")
        void shouldNotChangeArchivedResourceTwice() {
            Resource resource = createValidResource();
            resource.archive();

            resource.archive();

            assertThat(resource.getStatus()).isEqualTo(ResourceStatus.ARCHIVED);
        }
    }

    @Nested
    @DisplayName("Update Details")
    class UpdateDetails {

        @Test
        @DisplayName("Should update resource details")
        void shouldUpdateResourceDetails() {
            Resource resource = createValidResource();
            ResourceDetails newDetails = new ResourceDetails("New Name", "New Description");

            resource.updateDetails(newDetails);

            assertThat(resource.getName()).isEqualTo("New Name");
            assertThat(resource.getDescription()).isEqualTo("New Description");
        }

        @Test
        @DisplayName("Should throw exception when details is null in updateDetails")
        void shouldThrowExceptionWhenUpdateDetailsNull() {
            Resource resource = createValidResource();

            assertThatThrownBy(() -> resource.updateDetails(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("details cannot be null");
        }

        @Test
        @DisplayName("Should not update details of archived resource")
        void shouldNotUpdateDetailsOfArchivedResource() {
            Resource resource = createValidResource();
            resource.archive();

            ResourceDetails newDetails = new ResourceDetails("New Name", "New Description");
            assertThatThrownBy(() -> resource.updateDetails(newDetails))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessage("Archived resources cannot be changed");
        }
    }

    @Nested
    @DisplayName("Equals and HashCode")
    class Equality {
        @Test
        @DisplayName("Should verify equals and hashCode contract")
        void shouldVerifyEqualsAndHashCode() {
            EqualsVerifier.forClass(Resource.class)
                    .withOnlyTheseFields("publicId")
                    .suppress(Warning.NONFINAL_FIELDS)
                    .verify();
        }
    }
}
