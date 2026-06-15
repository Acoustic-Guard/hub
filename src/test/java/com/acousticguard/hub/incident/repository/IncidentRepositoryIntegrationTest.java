package com.acousticguard.hub.incident.repository;

import com.acousticguard.hub.BaseIntegrationTest;
import com.acousticguard.hub.common.enums.IncidentStatus;
import com.acousticguard.hub.incident.model.Incident;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for IncidentRepository using Testcontainers with PostgreSQL.
 * Tests custom spatial queries and JPA repository methods.
 * 
 * NOTE: These tests require Docker Desktop to be running with proper daemon configuration.
 * Testcontainers needs to be able to connect to the Docker daemon to pull and run containers.
 * If you see ContainerFetchException, ensure Docker Desktop is running and accessible.
 */
@SpringBootTest
@Disabled("Docker daemon not accessible to Testcontainers - requires Docker Desktop configuration")
class IncidentRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private IncidentRepository incidentRepository;

    private GeometryFactory geometryFactory;

    @BeforeEach
    void setUp() {
        geometryFactory = new GeometryFactory();
        incidentRepository.deleteAll();
    }

    @Nested
    @DisplayName("When finding nearby active incidents")
    class WhenFindingNearbyActiveIncidents {

        @Test
        @DisplayName("Should find incidents within spatial threshold")
        void shouldFindIncidentsWithinSpatialThreshold() {
            // Arrange
            Point centerPoint = geometryFactory.createPoint(new Coordinate(35.5, 48.5));
            Incident nearbyIncident = createIncident(48.5f, 35.5f, IncidentStatus.DETECTED.getValue());
            Incident farIncident = createIncident(49.0f, 36.0f, IncidentStatus.DETECTED.getValue());
            Incident resolvedIncident = createIncident(48.5f, 35.51f, IncidentStatus.RESOLVED.getValue());
            
            incidentRepository.save(nearbyIncident);
            incidentRepository.save(farIncident);
            incidentRepository.save(resolvedIncident);

            Instant timeThreshold = Instant.now().minusSeconds(300);

            // Act
            List<Incident> result = incidentRepository.findNearbyActiveIncidents(
                    48.5, 35.5, 500.0, "Explosion", timeThreshold
            );

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(nearbyIncident.getId());
        }

        @Test
        @DisplayName("Should not find incidents beyond spatial threshold")
        void shouldNotFindIncidentsBeyondSpatialThreshold() {
            // Arrange
            Incident farIncident = createIncident(49.0f, 36.0f, IncidentStatus.DETECTED.getValue());
            incidentRepository.save(farIncident);

            Instant timeThreshold = Instant.now().minusSeconds(300);

            // Act
            List<Incident> result = incidentRepository.findNearbyActiveIncidents(
                    48.5, 35.5, 500.0, "Explosion", timeThreshold
            );

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should not find resolved incidents")
        void shouldNotFindResolvedIncidents() {
            // Arrange
            Incident resolvedIncident = createIncident(48.5f, 35.51f, IncidentStatus.RESOLVED.getValue());
            incidentRepository.save(resolvedIncident);

            Instant timeThreshold = Instant.now().minusSeconds(300);

            // Act
            List<Incident> result = incidentRepository.findNearbyActiveIncidents(
                    48.5, 35.5, 500.0, "Explosion", timeThreshold
            );

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should not find incidents outside temporal threshold")
        void shouldNotFindIncidentsOutsideTemporalThreshold() {
            // Arrange
            Incident oldIncident = createIncident(48.5f, 35.5f, IncidentStatus.DETECTED.getValue());
            oldIncident.setCreatedAt(Instant.now().minusSeconds(600)); // 10 minutes ago
            incidentRepository.save(oldIncident);

            Instant timeThreshold = Instant.now().minusSeconds(300);

            // Act
            List<Incident> result = incidentRepository.findNearbyActiveIncidents(
                    48.5, 35.5, 500.0, "Explosion", timeThreshold
            );

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should filter by threat type")
        void shouldFilterByThreatType() {
            // Arrange
            Incident explosionIncident = createIncident(48.5f, 35.5f, IncidentStatus.DETECTED.getValue());
            explosionIncident.setType("Explosion");
            
            Incident uavIncident = createIncident(48.51f, 35.51f, IncidentStatus.DETECTED.getValue());
            uavIncident.setType("UAV");
            
            incidentRepository.save(explosionIncident);
            incidentRepository.save(uavIncident);

            Instant timeThreshold = Instant.now().minusSeconds(300);

            // Act
            List<Incident> result = incidentRepository.findNearbyActiveIncidents(
                    48.5, 35.5, 500.0, "Explosion", timeThreshold
            );

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getType()).isEqualTo("Explosion");
        }
    }

    @Nested
    @DisplayName("When finding incidents by status")
    class WhenFindingIncidentsByStatus {

        @Test
        @DisplayName("Should find incidents not in RESOLVED status")
        void shouldFindIncidentsNotInResolvedStatus() {
            // Arrange
            Incident detectedIncident = createIncident(48.5f, 35.5f, IncidentStatus.DETECTED.getValue());
            Incident investigatingIncident = createIncident(48.6f, 35.6f, IncidentStatus.INVESTIGATING.getValue());
            Incident resolvedIncident = createIncident(48.7f, 35.7f, IncidentStatus.RESOLVED.getValue());
            
            incidentRepository.save(detectedIncident);
            incidentRepository.save(investigatingIncident);
            incidentRepository.save(resolvedIncident);

            // Act
            List<Incident> result = incidentRepository.findByStatusNot(IncidentStatus.RESOLVED.getValue());

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(incident -> !incident.getStatus().equals(IncidentStatus.RESOLVED.getValue()));
        }

        @Test
        @DisplayName("Should return empty list when all incidents resolved")
        void shouldReturnEmptyListWhenAllIncidentsResolved() {
            // Arrange
            Incident resolvedIncident1 = createIncident(48.5f, 35.5f, IncidentStatus.RESOLVED.getValue());
            Incident resolvedIncident2 = createIncident(48.6f, 35.6f, IncidentStatus.RESOLVED.getValue());
            
            incidentRepository.save(resolvedIncident1);
            incidentRepository.save(resolvedIncident2);

            // Act
            List<Incident> result = incidentRepository.findByStatusNot(IncidentStatus.RESOLVED.getValue());

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("When finding incidents within bounding box")
    class WhenFindingIncidentsWithinBoundingBox {

        @Test
        @DisplayName("Should find incidents within bounding box")
        void shouldFindIncidentsWithinBoundingBox() {
            // Arrange
            Incident insideIncident1 = createIncident(48.5f, 35.5f, IncidentStatus.DETECTED.getValue());
            Incident insideIncident2 = createIncident(48.6f, 35.6f, IncidentStatus.DETECTED.getValue());
            Incident outsideIncident = createIncident(49.0f, 36.0f, IncidentStatus.DETECTED.getValue());
            
            incidentRepository.save(insideIncident1);
            incidentRepository.save(insideIncident2);
            incidentRepository.save(outsideIncident);

            // Act
            List<Incident> result = incidentRepository.findActiveWithinBbox(48.4f, 48.7f, 35.4f, 35.7f);

            // Assert
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should not find incidents outside bounding box")
        void shouldNotFindIncidentsOutsideBoundingBox() {
            // Arrange
            Incident outsideIncident = createIncident(49.0f, 36.0f, IncidentStatus.DETECTED.getValue());
            incidentRepository.save(outsideIncident);

            // Act
            List<Incident> result = incidentRepository.findActiveWithinBbox(48.4f, 48.7f, 35.4f, 35.7f);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should exclude resolved incidents from bbox query")
        void shouldExcludeResolvedIncidentsFromBboxQuery() {
            // Arrange
            Incident activeIncident = createIncident(48.5f, 35.5f, IncidentStatus.DETECTED.getValue());
            Incident resolvedIncident = createIncident(48.6f, 35.6f, IncidentStatus.RESOLVED.getValue());
            
            incidentRepository.save(activeIncident);
            incidentRepository.save(resolvedIncident);

            // Act
            List<Incident> result = incidentRepository.findActiveWithinBbox(48.4f, 48.7f, 35.4f, 35.7f);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(IncidentStatus.DETECTED.getValue());
        }
    }

    @Nested
    @DisplayName("When saving and retrieving incidents")
    class WhenSavingAndRetrievingIncidents {

        @Test
        @DisplayName("Should persist and retrieve incident with spatial data")
        void shouldPersistAndRetrieveIncidentWithSpatialData() {
            // Arrange
            Incident incident = createIncident(48.5f, 35.5f, IncidentStatus.DETECTED.getValue());

            // Act
            Incident saved = incidentRepository.save(incident);
            Incident retrieved = incidentRepository.findById(saved.getId()).orElse(null);

            // Assert
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getId()).isEqualTo(saved.getId());
            assertThat(retrieved.getLocationGeo()).isNotNull();
            assertThat(retrieved.getLocationGeo().getX()).isEqualTo(35.5);
            assertThat(retrieved.getLocationGeo().getY()).isEqualTo(48.5);
        }

        @Test
        @DisplayName("Should update incident status")
        void shouldUpdateIncidentStatus() {
            // Arrange
            Incident incident = createIncident(48.5f, 35.5f, IncidentStatus.DETECTED.getValue());
            Incident saved = incidentRepository.save(incident);

            // Act
            saved.setStatus(IncidentStatus.INVESTIGATING.getValue());
            Incident updated = incidentRepository.save(saved);
            Incident retrieved = incidentRepository.findById(updated.getId()).orElse(null);

            // Assert
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getStatus()).isEqualTo(IncidentStatus.INVESTIGATING.getValue());
        }

        @Test
        @DisplayName("Should store and retrieve metadata")
        void shouldStoreAndRetrieveMetadata() {
            // Arrange
            Incident incident = createIncident(48.5f, 35.5f, IncidentStatus.DETECTED.getValue());
            incident.setMetadata(java.util.Map.of(
                    "alertCount", 5,
                    "firstAlertId", UUID.randomUUID().toString()
            ));

            // Act
            Incident saved = incidentRepository.save(incident);
            Incident retrieved = incidentRepository.findById(saved.getId()).orElse(null);

            // Assert
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getMetadata()).isNotNull();
            assertThat(retrieved.getMetadata().get("alertCount")).isEqualTo(5);
        }
    }

    // Helper methods
    private Incident createIncident(float latitude, float longitude, String status) {
        Point location = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        return Incident.builder()
                .id(UUID.randomUUID())
                .locationGeo(location)
                .type("Explosion")
                .intensity(0.7f)
                .status(status)
                .sensorId("sensor-1")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .metadata(java.util.Map.of("alertCount", 1))
                .build();
    }
}
