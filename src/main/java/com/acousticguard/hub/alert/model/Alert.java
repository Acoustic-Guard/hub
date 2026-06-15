package com.acousticguard.hub.alert.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * JPA entity representing a threat alert in the Acoustic Guard system.
 * <p>
 * Alerts are generated when the threat classification service detects potential threats
 * in audio frames with confidence above the configured threshold. Each alert contains
 * the threat type, confidence score, sensor location, detection timestamp, and metadata
 * about the classification model. Alerts are persisted to the alerts table and may be
 * aggregated into incidents based on spatial and temporal proximity.
 * </p>
 */
@Entity
@Table(name = "alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    /**
     * Unique identifier for the alert.
     * Generated as a UUID by the database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The type of threat detected (e.g., "GUNSHOT", "SCREAM", "GLASS_BREAK").
     * This value is provided by the threat classification service.
     */
    @Column(name = "threat_type", nullable = false, length = 50)
    private String threatType;

    /**
     * The confidence score of the threat classification.
     * Ranges from 0.0 to 1.0, where higher values indicate higher confidence.
     * Alerts are only created if this value meets or exceeds the configured threshold.
     */
    @Column(nullable = false)
    private Float confidence;

    /**
     * Human-readable location description (e.g., "Main Street, Downtown").
     * This is a formatted string representation of the sensor's location.
     */
    @Column(nullable = false)
    private String location;

    /**
     * The timestamp when the threat was detected.
     * This corresponds to the capture timestamp of the audio frame that triggered the alert.
     */
    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    /**
     * The unique identifier of the sensor that detected the threat.
     * This references the sensor node that captured the audio frame.
     */
    @Column(name = "sensor_id", length = 50)
    private String sensorId;

    /**
     * The geographic location of the alert as a PostGIS Point.
     * Stored in EPSG:4326 (WGS 84) coordinate system for spatial queries.
     * Used for spatial correlation and bounding box filtering.
     */
    @Column(name = "location_geo", columnDefinition = "geography(Point, 4326)", nullable = false)
    private Point locationGeo;

    /**
     * Additional metadata about the alert and classification.
     * Stored as JSONB for flexible schema evolution. Contains information such as
     * model version, sample rate, FFT bin count, and peak decibel level.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
}