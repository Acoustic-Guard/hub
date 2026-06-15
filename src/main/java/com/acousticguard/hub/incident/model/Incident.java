package com.acousticguard.hub.incident.model;

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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * JPA entity representing an incident in the Acoustic Guard system.
 * <p>
 * Incidents are aggregations of alerts that occur within spatial and temporal proximity.
 * Multiple alerts from the same threat type within 500 meters and 300 seconds are
 * correlated into a single incident. The incident intensity is the maximum confidence
 * of all aggregated alerts, and the status is automatically escalated based on
 * accumulated intensity. Incidents are persisted to the incidents table with
 * PostGIS spatial support for geographic queries.
 * </p>
 */
@Entity
@Table(name = "incidents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident {

    /**
     * Unique identifier for the incident.
     * Generated as a UUID by the database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The geographic location of the incident as a PostGIS Point.
     * Stored in EPSG:4326 (WGS 84) coordinate system for spatial queries.
     * This represents the centroid location of the aggregated alerts.
     */
    @Column(name = "location_geo", columnDefinition = "geography(Point, 4326)", nullable = false)
    private Point locationGeo;

    /**
     * The type of threat associated with this incident.
     * This matches the threat type of the aggregated alerts (e.g., "GUNSHOT", "SCREAM").
     */
    @Column(nullable = false, length = 50)
    private String type;

    /**
     * The intensity of the incident, representing the maximum confidence
     * of all aggregated alerts. Ranges from 0.0 to 1.0.
     * Used for automatic status escalation based on intensity thresholds.
     */
    @Column(nullable = false)
    private Float intensity;

    /**
     * The current status of the incident.
     * Possible values include DETECTED, INVESTIGATING, CONFIRMED, and RESOLVED.
     * Status is automatically escalated based on accumulated intensity.
     */
    @Column(nullable = false, length = 50)
    private String status;

    /**
     * The unique identifier of the sensor associated with this incident.
     * This references the sensor node that first detected the threat.
     */
    @Column(name = "sensor_id", length = 50)
    private String sensorId;

    /**
     * The timestamp when the incident was first created.
     * Automatically set by Hibernate when the entity is persisted.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    /**
     * The timestamp when the incident was last updated.
     * Automatically updated by Hibernate on each entity update.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Additional metadata about the incident.
     * Stored as JSONB for flexible schema evolution. Contains information such as
     * first alert ID, last alert ID, alert count, and alert timestamps.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
}