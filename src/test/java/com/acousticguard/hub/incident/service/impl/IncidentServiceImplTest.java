package com.acousticguard.hub.incident.service.impl;

import com.acousticguard.hub.alert.model.Alert;
import com.acousticguard.hub.common.enums.IncidentStatus;
import com.acousticguard.hub.incident.mapper.IncidentMapper;
import com.acousticguard.hub.incident.model.Incident;
import com.acousticguard.hub.incident.repository.IncidentRepository;
import com.acousticguard.hub.websocket.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IncidentService Tests")
class IncidentServiceImplTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private IncidentMapper incidentMapper;

    @InjectMocks
    private IncidentServiceImpl incidentService;

    private GeometryFactory geometryFactory;

    @BeforeEach
    void setUp() {
        geometryFactory = new GeometryFactory();
        ReflectionTestUtils.setField(incidentService, "spatialThresholdMeters", 500.0);
        ReflectionTestUtils.setField(incidentService, "temporalThresholdSeconds", 300L);
    }

    @Nested
    @DisplayName("When receiving new alert")
    class WhenReceivingNewAlert {

        @Nested
        @DisplayName("Given no nearby incidents exist")
        class GivenNoNearbyIncidentsExist {

            @Test
            @DisplayName("Should create new incident with DETECTED status")
            void shouldCreateNewIncidentWithDetectedStatus() {
                // Arrange
                Alert alert = createAlert("sensor-1", 48.5f, 35.5f, 0.7f);
                when(incidentRepository.findNearbyActiveIncidents(
                        any(Double.class), any(Double.class), any(Double.class),
                        any(String.class), any(Instant.class)
                )).thenReturn(List.of());
                when(incidentMapper.latitudeLongitudeToPoint(any(Float.class), any(Float.class)))
                        .thenReturn(createPoint(48.5f, 35.5f));
                when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));
                when(incidentMapper.toDto(any(Incident.class))).thenReturn(null);

                // Act
                Incident result = incidentService.aggregateOrUpdate(alert);

                // Assert
                assertThat(result.getStatus()).isEqualTo(IncidentStatus.DETECTED.getValue());
                assertThat(result.getType()).isEqualTo(alert.getThreatType());
                assertThat(result.getIntensity()).isEqualTo(alert.getConfidence());
                verify(incidentRepository).save(any(Incident.class));
                verify(eventPublisher).publishIncident(any());
            }

            @Test
            @DisplayName("Should build initial metadata with alert information")
            void shouldBuildInitialMetadataWithAlertInformation() {
                // Arrange
                Alert alert = createAlert("sensor-1", 48.5f, 35.5f, 0.7f);
                when(incidentRepository.findNearbyActiveIncidents(
                        any(Double.class), any(Double.class), any(Double.class),
                        any(String.class), any(Instant.class)
                )).thenReturn(List.of());
                when(incidentMapper.latitudeLongitudeToPoint(any(Float.class), any(Float.class)))
                        .thenReturn(createPoint(48.5f, 35.5f));
                when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));
                when(incidentMapper.toDto(any(Incident.class))).thenReturn(null);

                // Act
                Incident result = incidentService.aggregateOrUpdate(alert);

                // Assert
                assertThat(result.getMetadata()).isNotNull();
                assertThat(result.getMetadata()).containsKey("firstAlertId");
                assertThat(result.getMetadata()).containsKey("firstAlertAt");
                assertThat(result.getMetadata()).containsKey("alertCount");
                assertThat(result.getMetadata().get("alertCount")).isEqualTo(1);
            }
        }

        @Nested
        @DisplayName("Given nearby incident exists")
        class GivenNearbyIncidentExists {

            @Test
            @DisplayName("Should update existing incident with higher intensity")
            void shouldUpdateExistingIncidentWithHigherIntensity() {
                // Arrange
                Alert alert = createAlert("sensor-1", 48.5f, 35.5f, 0.85f);
                Incident existingIncident = createIncident(UUID.randomUUID(), 48.5f, 35.5f, 0.7f, IncidentStatus.DETECTED.getValue());
                
                when(incidentRepository.findNearbyActiveIncidents(
                        any(Double.class), any(Double.class), any(Double.class),
                        any(String.class), any(Instant.class)
                )).thenReturn(List.of(existingIncident));
                when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));
                when(incidentMapper.toDto(any(Incident.class))).thenReturn(null);

                // Act
                Incident result = incidentService.aggregateOrUpdate(alert);

                // Assert
                assertThat(result.getIntensity()).isEqualTo(0.85f); // Max of 0.7 and 0.85
                assertThat(result.getStatus()).isEqualTo(IncidentStatus.INVESTIGATING.getValue()); // >= 0.8
                verify(incidentRepository).save(any(Incident.class));
                verify(eventPublisher).publishIncident(any());
            }

            @Test
            @DisplayName("Should transition to CONFIRMED when intensity >= 0.9")
            void shouldTransitionToConfirmedWhenIntensityHigh() {
                // Arrange
                Alert alert = createAlert("sensor-1", 48.5f, 35.5f, 0.95f);
                Incident existingIncident = createIncident(UUID.randomUUID(), 48.5f, 35.5f, 0.8f, IncidentStatus.INVESTIGATING.getValue());
                
                when(incidentRepository.findNearbyActiveIncidents(
                        any(Double.class), any(Double.class), any(Double.class),
                        any(String.class), any(Instant.class)
                )).thenReturn(List.of(existingIncident));
                when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));
                when(incidentMapper.toDto(any(Incident.class))).thenReturn(null);

                // Act
                Incident result = incidentService.aggregateOrUpdate(alert);

                // Assert
                assertThat(result.getIntensity()).isEqualTo(0.95f);
                assertThat(result.getStatus()).isEqualTo(IncidentStatus.CONFIRMED.getValue());
            }

            @Test
            @DisplayName("Should update metadata with latest alert information")
            void shouldUpdateMetadataWithLatestAlertInformation() {
                // Arrange
                Alert alert = createAlert("sensor-1", 48.5f, 35.5f, 0.85f);
                Incident existingIncident = createIncident(UUID.randomUUID(), 48.5f, 35.5f, 0.7f, IncidentStatus.DETECTED.getValue());
                existingIncident.getMetadata().put("alertCount", 3);
                
                when(incidentRepository.findNearbyActiveIncidents(
                        any(Double.class), any(Double.class), any(Double.class),
                        any(String.class), any(Instant.class)
                )).thenReturn(List.of(existingIncident));
                when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));
                when(incidentMapper.toDto(any(Incident.class))).thenReturn(null);

                // Act
                Incident result = incidentService.aggregateOrUpdate(alert);

                // Assert
                assertThat(result.getMetadata()).containsKey("lastAlertId");
                assertThat(result.getMetadata()).containsKey("lastAlertAt");
                assertThat(result.getMetadata().get("alertCount")).isEqualTo(4); // Incremented
            }
        }
    }

    @Nested
    @DisplayName("When finding active incidents")
    class WhenFindingActiveIncidents {

        @Test
        @DisplayName("Should return incidents not in RESOLVED status")
        void shouldReturnIncidentsNotInResolvedStatus() {
            // Arrange
            Incident activeIncident = createIncident(UUID.randomUUID(), 48.5f, 35.5f, 0.7f, IncidentStatus.INVESTIGATING.getValue());
            Incident resolvedIncident = createIncident(UUID.randomUUID(), 48.6f, 35.6f, 0.8f, IncidentStatus.RESOLVED.getValue());
            
            when(incidentRepository.findByStatusNot(IncidentStatus.RESOLVED.getValue()))
                        .thenReturn(List.of(activeIncident, resolvedIncident));

            // Act
            List<Incident> result = incidentService.findAllActive();

            // Assert
            assertThat(result).hasSize(2);
            verify(incidentRepository).findByStatusNot(IncidentStatus.RESOLVED.getValue());
        }
    }

    @Nested
    @DisplayName("When updating incident status")
    class WhenUpdatingIncidentStatus {

        @Test
        @DisplayName("Should update status and publish event")
        void shouldUpdateStatusAndPublishEvent() {
            // Arrange
            UUID incidentId = UUID.randomUUID();
            Incident existingIncident = createIncident(incidentId, 48.5f, 35.5f, 0.7f, IncidentStatus.INVESTIGATING.getValue());
            
            when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(existingIncident));
            when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(incidentMapper.toDto(any(Incident.class))).thenReturn(null);

            // Act
            Incident result = incidentService.updateStatus(incidentId, IncidentStatus.RESOLVED.getValue());

            // Assert
            assertThat(result.getStatus()).isEqualTo(IncidentStatus.RESOLVED.getValue());
            verify(incidentRepository).save(any(Incident.class));
            verify(eventPublisher).publishIncident(any());
        }

        @Test
        @DisplayName("Should throw exception when incident not found")
        void shouldThrowExceptionWhenIncidentNotFound() {
            // Arrange
            UUID incidentId = UUID.randomUUID();
            when(incidentRepository.findById(incidentId)).thenReturn(Optional.empty());

            // Act & Assert
            org.junit.jupiter.api.Assertions.assertThrows(
                    com.acousticguard.hub.common.error.IncidentNotFoundError.class,
                    () -> incidentService.updateStatus(incidentId, IncidentStatus.RESOLVED.getValue())
            );
        }
    }

    @Nested
    @DisplayName("When finding incidents within bounding box")
    class WhenFindingIncidentsWithinBoundingBox {

        @Test
        @DisplayName("Should call repository with correct parameters")
        void shouldCallRepositoryWithCorrectParameters() {
            // Arrange
            float minLat = 48.0f;
            float maxLat = 49.0f;
            float minLng = 35.0f;
            float maxLng = 36.0f;
            
            when(incidentRepository.findActiveWithinBbox(minLng, minLat, maxLng, maxLat))
                    .thenReturn(List.of());

            // Act
            incidentService.findActiveWithinBbox(minLat, maxLat, minLng, maxLng);

            // Assert
            verify(incidentRepository).findActiveWithinBbox(minLng, minLat, maxLng, maxLat);
        }
    }

    @TestFactory
    @DisplayName("Dynamic tests for intensity-based status transitions")
    Stream<org.junit.jupiter.api.DynamicTest> intensityStatusTransitions() {
        return Stream.of(
                org.junit.jupiter.api.DynamicTest.dynamicTest(
                        "Intensity 0.7 should remain DETECTED",
                        () -> {
                            Alert alert = createAlert("sensor-1", 48.5f, 35.5f, 0.7f);
                            Incident existingIncident = createIncident(UUID.randomUUID(), 48.5f, 35.5f, 0.6f, IncidentStatus.DETECTED.getValue());
                            
                            when(incidentRepository.findNearbyActiveIncidents(
                                    any(Double.class), any(Double.class), any(Double.class),
                                    any(String.class), any(Instant.class)
                            )).thenReturn(List.of(existingIncident));
                            when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));
                            when(incidentMapper.toDto(any(Incident.class))).thenReturn(null);

                            Incident result = incidentService.aggregateOrUpdate(alert);
                            assertThat(result.getStatus()).isEqualTo(IncidentStatus.DETECTED.getValue());
                        }
                ),
                org.junit.jupiter.api.DynamicTest.dynamicTest(
                        "Intensity 0.8 should transition to INVESTIGATING",
                        () -> {
                            Alert alert = createAlert("sensor-1", 48.5f, 35.5f, 0.8f);
                            Incident existingIncident = createIncident(UUID.randomUUID(), 48.5f, 35.5f, 0.7f, IncidentStatus.DETECTED.getValue());
                            
                            when(incidentRepository.findNearbyActiveIncidents(
                                    any(Double.class), any(Double.class), any(Double.class),
                                    any(String.class), any(Instant.class)
                            )).thenReturn(List.of(existingIncident));
                            when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));
                            when(incidentMapper.toDto(any(Incident.class))).thenReturn(null);

                            Incident result = incidentService.aggregateOrUpdate(alert);
                            assertThat(result.getStatus()).isEqualTo(IncidentStatus.INVESTIGATING.getValue());
                        }
                ),
                org.junit.jupiter.api.DynamicTest.dynamicTest(
                        "Intensity 0.9 should transition to CONFIRMED",
                        () -> {
                            Alert alert = createAlert("sensor-1", 48.5f, 35.5f, 0.9f);
                            Incident existingIncident = createIncident(UUID.randomUUID(), 48.5f, 35.5f, 0.8f, IncidentStatus.INVESTIGATING.getValue());
                            
                            when(incidentRepository.findNearbyActiveIncidents(
                                    any(Double.class), any(Double.class), any(Double.class),
                                    any(String.class), any(Instant.class)
                            )).thenReturn(List.of(existingIncident));
                            when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));
                            when(incidentMapper.toDto(any(Incident.class))).thenReturn(null);

                            Incident result = incidentService.aggregateOrUpdate(alert);
                            assertThat(result.getStatus()).isEqualTo(IncidentStatus.CONFIRMED.getValue());
                        }
                ),
                org.junit.jupiter.api.DynamicTest.dynamicTest(
                        "Intensity 1.0 should transition to CONFIRMED",
                        () -> {
                            Alert alert = createAlert("sensor-1", 48.5f, 35.5f, 1.0f);
                            Incident existingIncident = createIncident(UUID.randomUUID(), 48.5f, 35.5f, 0.9f, IncidentStatus.INVESTIGATING.getValue());
                            
                            when(incidentRepository.findNearbyActiveIncidents(
                                    any(Double.class), any(Double.class), any(Double.class),
                                    any(String.class), any(Instant.class)
                            )).thenReturn(List.of(existingIncident));
                            when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));
                            when(incidentMapper.toDto(any(Incident.class))).thenReturn(null);

                            Incident result = incidentService.aggregateOrUpdate(alert);
                            assertThat(result.getStatus()).isEqualTo(IncidentStatus.CONFIRMED.getValue());
                        }
                )
        );
    }

    // Helper methods
    private Alert createAlert(String sensorId, float latitude, float longitude, float confidence) {
        Alert alert = new Alert();
        alert.setId(UUID.randomUUID());
        alert.setSensorId(sensorId);
        alert.setThreatType("Explosion");
        alert.setConfidence(confidence);
        alert.setDetectedAt(Instant.now());
        alert.setLocation(String.format("%.6f,%.6f", latitude, longitude));
        alert.setLocationGeo(createPoint(latitude, longitude));
        return alert;
    }

    private Incident createIncident(UUID id, float latitude, float longitude, float intensity, String status) {
        java.util.Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("alertCount", 1);
        return Incident.builder()
                .id(id)
                .locationGeo(createPoint(latitude, longitude))
                .type("Explosion")
                .intensity(intensity)
                .status(status)
                .sensorId("sensor-1")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .metadata(metadata)
                .build();
    }

    private Point createPoint(float latitude, float longitude) {
        return geometryFactory.createPoint(new Coordinate(longitude, latitude));
    }
}
