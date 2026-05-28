package com.acousticguard.hub.alert.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "threat_type", nullable = false, length = 50)
    private String threatType;

    @Column(nullable = false)
    private Float confidence;

    @Column(nullable = false)
    private String location;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "sensor_id", length = 50)
    private String sensorId;

    private Float latitude;
    private Float longitude;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
}