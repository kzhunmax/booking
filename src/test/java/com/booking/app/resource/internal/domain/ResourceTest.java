package com.booking.app.resource.internal.domain;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.booking.app.resource.InvalidStatusTransitionException;
import com.booking.app.resource.ResourceStatus;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ResourceTest {

    @Nested
    @DisplayName("Resource Creation")
    class Creation {

        @Test
        @DisplayName("Should create resource with valid name")
        void shouldCreateResourceWithValidName() {
            String name = "Conference Room A";
            String description = "Large Meeting Room";

            Resource resource = new Resource(name, description);

            assertThat(resource.getName()).isEqualTo(name);
            assertThat(resource.getDescription()).isEqualTo(description);
            assertThat(resource.getStatus()).isEqualTo(ResourceStatus.ACTIVE);
            assertThat(resource.getPublicId()).isNotNull();
            assertThat(resource.getAuditInfo()).isNotNull();

            // Hibernate generates
            assertThat(resource.getId()).isNull();
            assertThat(resource.getVersion()).isNull();
            assertThat(resource.getAuditInfo().getCreatedAt()).isNull();
            assertThat(resource.getAuditInfo().getUpdatedAt()).isNull();
        }

        @Test
        @DisplayName("Should throw exception for blank name")
        void shouldThrowExceptionForBlankName() {
            assertThatThrownBy(() -> new Resource("", "description"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Name cannot be blank");
        }

        @Test
        @DisplayName("Should throw exception for null name")
        void shouldThrowExceptionForNullName() {
            assertThatThrownBy(() -> new Resource(null, "description"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Name cannot be null");
        }

        @Test
        @DisplayName("Should throw exception for name > 255 characters")
        void shouldThrowExceptionForNameLarger255() {
            assertThatThrownBy(() -> new Resource("a".repeat(256), "description"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Name cannot exceed 255 characters");
        }

        @Test
        @DisplayName("Should trim name whitespace")
        void shouldTrimNameWhitespace() {
            Resource resource = new Resource("   Room A  ", "description");
            assertThat(resource.getName()).isEqualTo("Room A");
        }
    }

    @Nested
    @DisplayName("Status Transitions")
    class StatusTransitions {

        @Test
        @DisplayName("Should activate inactive resource")
        void shouldActivateInactiveResource() {
            Resource resource = new Resource("Room", "description");
            resource.deactivate();

            resource.activate();

            assertThat(resource.getStatus()).isEqualTo(ResourceStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should not activate archived resource")
        void shouldNotActivateArchivedResource() {
            Resource resource = new Resource("Room", "description");
            resource.archive();

            assertThatThrownBy(resource::activate)
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessage("Archived resources cannot be changed");
        }

        @Test
        @DisplayName("Should archive active resource")
        void shouldArchiveActiveResource() {
            Resource resource = new Resource("Room", "description");

            resource.archive();

            assertThat(resource.getStatus()).isEqualTo(ResourceStatus.ARCHIVED);
        }

        @Test
        @DisplayName("Should not change archived resource twice")
        void shouldNotChangeArchivedResourceTwice() {
            Resource resource = new Resource("Room", "description");
            resource.archive();

            resource.archive();

            assertThat(resource.getStatus()).isEqualTo(ResourceStatus.ARCHIVED);
        }
    }

    @Nested
    @DisplayName("Rename")
    class Rename {

        @Test
        @DisplayName("Should rename resource")
        void shouldRenameResource() {
            Resource resource = new Resource("Old Name", "description");

            resource.rename("New Name");

            assertThat(resource.getName()).isEqualTo("New Name");
        }

        @Test
        @DisplayName("Should not rename to blank name")
        void shouldNotRenameToBlankName() {
            Resource resource = new Resource("Room", "description");

            assertThatThrownBy(() -> resource.rename("")).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should not rename or change description of archived resource")
        void shouldNotChangeArchivedResource() {
            Resource resource = new Resource("Room", "description");
            resource.archive();

            assertThatThrownBy(() -> resource.rename("New Name")).isInstanceOf(InvalidStatusTransitionException.class);
            assertThatThrownBy(() -> resource.changeDescription("New description"))
                    .isInstanceOf(InvalidStatusTransitionException.class);
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
