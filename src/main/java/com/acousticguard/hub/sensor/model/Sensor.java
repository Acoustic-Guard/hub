package com.acousticguard.hub.sensor.model;

import com.acousticguard.hub.common.enums.SensorStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * JPA entity representing a sensor node in the Acoustic Guard system.
 * <p>
 * Sensors are edge devices that capture audio data and transmit telemetry to the hub.
 * Each sensor has a unique identifier, geographic location, status (ONLINE/OFFLINE),
 * and current noise level. The entity tracks the last heartbeat timestamp and
 * maintains noise data for real-time monitoring. Sensors are persisted to the
 * sensors table and monitored by the health monitoring service for connectivity issues.
 * </p>
 */
@Entity
@Table(name = "sensors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sensor {

    /**
     * Unique identifier for the sensor.
     * This is the sensor's hardware ID assigned during registration.
     */
    @Id
    private String id;

    /**
     * Human-readable location description (e.g., "Main Street, Downtown").
     * This is a formatted string representation of the sensor's physical location.
     */
    @Column(nullable = false, length = 100)
    private String location;

    /**
     * GPS latitude coordinate of the sensor.
     * Used for geographic positioning and spatial queries.
     */
    @Column(nullable = false)
    private Float latitude;

    /**
     * GPS longitude coordinate of the sensor.
     * Used for geographic positioning and spatial queries.
     */
    @Column(nullable = false)
    private Float longitude;

    /**
     * Current operational status of the sensor.
     * Possible values are ONLINE (actively transmitting) and OFFLINE (not transmitting).
     * Status is automatically updated by the health monitoring service based on heartbeat activity.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SensorStatus status;

    /**
     * Timestamp of the last heartbeat received from this sensor.
     * Used by the health monitoring service to detect offline sensors.
     */
    @Column(name = "last_heartbeat")
    private Instant lastHeartbeat;

    /**
     * Current average decibel level measured by the sensor.
     * This value is updated periodically from incoming telemetry data.
     * Null if no noise data has been received recently.
     */
    @Column(name = "current_avg_db")
    private Float currentAvgDb;

    /**
     * Timestamp when the noise data was last updated.
     * Used to determine the freshness of the current average decibel level.
     */
    @Column(name = "noise_updated_at")
    private Instant noiseUpdatedAt;

    /**
     * Timestamp when the sensor was first registered in the system.
     * Automatically set on entity creation and never updated.
     */
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    /**
     * Timestamp when the sensor was last updated.
     * Automatically updated on each entity modification.
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Lifecycle callback executed before entity persistence.
     * Initializes timestamps and sets default status to OFFLINE if not specified.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) {
            status = SensorStatus.OFFLINE;
        }
    }

    /**
     * Lifecycle callback executed before entity update.
     * Updates the last modification timestamp.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
