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

/**
 * Spring Data JPA repository for Incident entity persistence.
 * <p>
 * Provides data access operations for incidents, including PostGIS spatial queries
 * for geographic filtering and analytics queries for threat distribution and intensity
 * metrics. This repository extends JpaRepository for standard CRUD operations and
 * JpaSpecificationExecutor for dynamic query building. Custom native queries leverage
 * PostGIS functions for efficient spatial correlation and bounding box filtering.
 * </p>
 */
@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID>, JpaSpecificationExecutor<Incident> {

    /**
     * Finds all incidents with status not equal to the specified value.
     * <p>
     * This method is used to retrieve active incidents (those not yet resolved).
     * </p>
     *
     * @param status the status to exclude (e.g., "RESOLVED")
     * @return a list of incidents with status not equal to the specified value
     */
    List<Incident> findByStatusNot(String status);

    /**
     * Finds nearby active incidents using PostGIS spatial query.
     * <p>
     * This method uses the PostGIS ST_DWithin function to find incidents within
     * a specified radius of a given point. Only incidents with the same threat type,
     * updated after the time threshold, and not resolved are returned. This is used
     * for alert aggregation to correlate nearby threats into incidents.
     * </p>
     *
     * @param latitude      the latitude of the center point
     * @param longitude     the longitude of the center point
     * @param radiusMeters  the search radius in meters
     * @param threatType    the threat type to match
     * @param timeThreshold the minimum update timestamp
     * @return a list of nearby active incidents, or empty if none found
     */
    @Query(value = "SELECT * FROM incidents WHERE ST_DWithin(location_geo, ST_MakePoint(:longitude, :latitude)::geography, :radiusMeters) = true AND type = :threatType AND updated_at > :timeThreshold AND status != 'RESOLVED'", nativeQuery = true)
    List<Incident> findNearbyActiveIncidents(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radiusMeters") double radiusMeters,
            @Param("threatType") String threatType,
            @Param("timeThreshold") Instant timeThreshold
    );

    /**
     * Finds active incidents within a geographic bounding box using PostGIS.
     * <p>
     * This method uses the PostGIS ST_MakeEnvelope function to create a bounding box
     * and the && operator to test for intersection. Only incidents with status not
     * equal to RESOLVED are returned. This enables efficient geographic filtering
     * for map-based incident visualization.
     * </p>
     *
     * @param minLng the minimum longitude (west boundary)
     * @param minLat the minimum latitude (south boundary)
     * @param maxLng the maximum longitude (east boundary)
     * @param maxLat the maximum latitude (north boundary)
     * @return a list of active incidents within the bounding box, or empty if none found
     */
    @Query(value = "SELECT * FROM incidents WHERE location_geo && ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326) AND status != 'RESOLVED'", nativeQuery = true)
    List<Incident> findActiveWithinBbox(
            @Param("minLng") double minLng,
            @Param("minLat") double minLat,
            @Param("maxLng") double maxLng,
            @Param("maxLat") double maxLat
    );

    /**
     * Counts incidents created after a specified timestamp.
     * <p>
     * This method is used for analytics to calculate incident rates over time.
     * </p>
     *
     * @param threshold the timestamp threshold (incidents created after this time)
     * @return the count of incidents created after the threshold
     */
    long countByCreatedAtAfter(Instant threshold);

    /**
     * Counts incidents created within a time range.
     * <p>
     * This method is used for analytics to calculate incident rates for specific
     * time periods (e.g., hourly, daily, weekly).
     * </p>
     *
     * @param start the start of the time range
     * @param end   the end of the time range
     * @return the count of incidents created within the time range
     */
    long countByCreatedAtBetween(Instant start, Instant end);

    /**
     * Counts high-intensity incidents created after a specified timestamp.
     * <p>
     * This method is used for analytics to track severe incidents.
     * </p>
     *
     * @param threshold the timestamp threshold
     * @param intensity the minimum intensity threshold
     * @return the count of high-intensity incidents created after the threshold
     */
    long countByCreatedAtAfterAndIntensityGreaterThanEqual(Instant threshold, Float intensity);

    /**
     * Counts high-intensity incidents created within a time range.
     * <p>
     * This method is used for analytics to track severe incidents in specific periods.
     * </p>
     *
     * @param start     the start of the time range
     * @param end       the end of the time range
     * @param intensity the minimum intensity threshold
     * @return the count of high-intensity incidents within the time range
     */
    long countByCreatedAtBetweenAndIntensityGreaterThanEqual(Instant start, Instant end, Float intensity);

    /**
     * Calculates the average intensity of incidents created after a timestamp.
     * <p>
     * This method is used for analytics to understand threat severity trends.
     * </p>
     *
     * @param threshold the timestamp threshold
     * @return the average intensity, or null if no incidents found
     */
    @Query("SELECT AVG(i.intensity) FROM Incident i WHERE i.createdAt > :threshold")
    Double averageIntensityByCreatedAtAfter(@Param("threshold") Instant threshold);

    /**
     * Calculates the average intensity of incidents created within a time range.
     * <p>
     * This method is used for analytics to understand threat severity trends in specific periods.
     * </p>
     *
     * @param start the start of the time range
     * @param end   the end of the time range
     * @return the average intensity, or null if no incidents found
     */
    @Query("SELECT AVG(i.intensity) FROM Incident i WHERE i.createdAt BETWEEN :start AND :end")
    Double averageIntensityByCreatedAtBetween(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * Finds threat distribution after a specified timestamp.
     * <p>
     * This method aggregates incidents by threat type for analytics dashboards.
     * Returns a list of DTOs containing threat type and count.
     * </p>
     *
     * @param threshold the timestamp threshold
     * @return a list of threat distribution DTOs
     */
    @Query("SELECT new com.acousticguard.hub.analytics.dto.ThreatDistributionDto(i.type, COUNT(i)) FROM Incident i WHERE i.createdAt > :threshold GROUP BY i.type")
    List<ThreatDistributionDto> findThreatDistributionByCreatedAtAfter(@Param("threshold") Instant threshold);

    /**
     * Finds threat distribution within a time range.
     * <p>
     * This method aggregates incidents by threat type for analytics dashboards.
     * Returns a list of DTOs containing threat type and count.
     * </p>
     *
     * @param start the start of the time range
     * @param end   the end of the time range
     * @return a list of threat distribution DTOs
     */
    @Query("SELECT new com.acousticguard.hub.analytics.dto.ThreatDistributionDto(i.type, COUNT(i)) FROM Incident i WHERE i.createdAt BETWEEN :start AND :end GROUP BY i.type")
    List<ThreatDistributionDto> findThreatDistributionByCreatedAtBetween(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * Finds the 50 most recent incidents created after a timestamp.
     * <p>
     * This method is used for displaying recent activity in dashboards.
     * </p>
     *
     * @param threshold the timestamp threshold
     * @return a list of up to 50 incidents ordered by creation time descending
     */
    List<Incident> findTop50ByCreatedAtAfterOrderByCreatedAtDesc(Instant threshold);

    /**
     * Finds the 50 most recent incidents created within a time range.
     * <p>
     * This method is used for displaying recent activity in dashboards.
     * </p>
     *
     * @param start the start of the time range
     * @param end   the end of the time range
     * @return a list of up to 50 incidents ordered by creation time descending
     */
    List<Incident> findTop50ByCreatedAtBetweenOrderByCreatedAtDesc(Instant start, Instant end);

    /**
     * Finds all incidents created after a timestamp, ordered by creation time ascending.
     * <p>
     * This method is used for time-series analytics and charting.
     * </p>
     *
     * @param threshold the timestamp threshold
     * @return a list of incidents ordered by creation time ascending
     */
    List<Incident> findByCreatedAtAfterOrderByCreatedAtAsc(Instant threshold);

    /**
     * Finds all incidents created within a time range, ordered by creation time ascending.
     * <p>
     * This method is used for time-series analytics and charting.
     * </p>
     *
     * @param start the start of the time range
     * @param end   the end of the time range
     * @return a list of incidents ordered by creation time ascending
     */
    List<Incident> findByCreatedAtBetweenOrderByCreatedAtAsc(Instant start, Instant end);
}