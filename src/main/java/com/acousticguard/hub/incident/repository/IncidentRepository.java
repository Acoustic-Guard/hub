package com.acousticguard.hub.incident.repository;

import com.acousticguard.hub.analytics.dto.ThreatDistributionDto;
import com.acousticguard.hub.incident.model.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID>, JpaSpecificationExecutor<Incident> {

    List<Incident> findByStatusNot(String status);

    @Query(value = "SELECT * FROM incidents WHERE ST_DWithin(location_geo, ST_MakePoint(:longitude, :latitude)::geography, :radiusMeters) = true AND type = :threatType AND updated_at > :timeThreshold AND status != 'RESOLVED'", nativeQuery = true)
    List<Incident> findNearbyActiveIncidents(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radiusMeters") double radiusMeters,
            @Param("threatType") String threatType,
            @Param("timeThreshold") Instant timeThreshold
    );

    @Query(value = "SELECT * FROM incidents WHERE location_geo && ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326) AND status != 'RESOLVED'", nativeQuery = true)
    List<Incident> findActiveWithinBbox(
            @Param("minLng") double minLng,
            @Param("minLat") double minLat,
            @Param("maxLng") double maxLng,
            @Param("maxLat") double maxLat
    );

    long countByCreatedAtAfter(Instant threshold);

    long countByCreatedAtBetween(Instant start, Instant end);

    long countByCreatedAtAfterAndIntensityGreaterThanEqual(Instant threshold, Float intensity);

    long countByCreatedAtBetweenAndIntensityGreaterThanEqual(Instant start, Instant end, Float intensity);

    @Query("SELECT AVG(i.intensity) FROM Incident i WHERE i.createdAt > :threshold")
    Double averageIntensityByCreatedAtAfter(@Param("threshold") Instant threshold);

    @Query("SELECT AVG(i.intensity) FROM Incident i WHERE i.createdAt BETWEEN :start AND :end")
    Double averageIntensityByCreatedAtBetween(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT new com.acousticguard.hub.analytics.dto.ThreatDistributionDto(i.type, COUNT(i)) FROM Incident i WHERE i.createdAt > :threshold GROUP BY i.type")
    List<ThreatDistributionDto> findThreatDistributionByCreatedAtAfter(@Param("threshold") Instant threshold);

    @Query("SELECT new com.acousticguard.hub.analytics.dto.ThreatDistributionDto(i.type, COUNT(i)) FROM Incident i WHERE i.createdAt BETWEEN :start AND :end GROUP BY i.type")
    List<ThreatDistributionDto> findThreatDistributionByCreatedAtBetween(@Param("start") Instant start, @Param("end") Instant end);

    List<Incident> findTop50ByCreatedAtAfterOrderByCreatedAtDesc(Instant threshold);

    List<Incident> findTop50ByCreatedAtBetweenOrderByCreatedAtDesc(Instant start, Instant end);

    List<Incident> findByCreatedAtAfterOrderByCreatedAtAsc(Instant threshold);

    List<Incident> findByCreatedAtBetweenOrderByCreatedAtAsc(Instant start, Instant end);
}