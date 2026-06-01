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

@Entity
@Table(name = "incidents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "location_geo", columnDefinition = "geography(Point, 4326)", nullable = false)
    private Point locationGeo;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false)
    private Float intensity;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "sensor_id", length = 50)
    private String sensorId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
}