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
 * Entity representing a sensor node in the Acoustic Guard system.
 */
@Entity
@Table(name = "sensors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sensor {

    @Id
    private String id;

    @Column(nullable = false, length = 100)
    private String location;

    @Column(nullable = false)
    private Float latitude;

    @Column(nullable = false)
    private Float longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SensorStatus status;

    @Column(name = "last_heartbeat")
    private Instant lastHeartbeat;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) {
            status = SensorStatus.OFFLINE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
