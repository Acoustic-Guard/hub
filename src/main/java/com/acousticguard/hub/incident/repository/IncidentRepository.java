package com.acousticguard.hub.incident.repository;

import com.acousticguard.hub.incident.model.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID>, JpaSpecificationExecutor<Incident> {

    @Query("SELECT i FROM Incident i WHERE i.latitude >= :minLat AND i.latitude <= :maxLat AND i.longitude >= :minLng AND i.longitude <= :maxLng AND i.status != 'Resolved'")
    List<Incident> findActiveWithinBbox(
            @Param("minLat") Float minLat,
            @Param("maxLat") Float maxLat,
            @Param("minLng") Float minLng,
            @Param("maxLng") Float maxLng
    );
}