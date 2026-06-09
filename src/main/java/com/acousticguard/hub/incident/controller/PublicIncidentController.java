package com.acousticguard.hub.incident.controller;

import com.acousticguard.hub.incident.dto.PublicIncidentResponseDto;
import com.acousticguard.hub.incident.service.IncidentService;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/incidents")
@RequiredArgsConstructor
public class PublicIncidentController {

    private final IncidentService incidentService;

    @GetMapping
    public ResponseEntity<List<PublicIncidentResponseDto>> getPublicIncidents() {
        List<PublicIncidentResponseDto> publicIncidents = incidentService.findAllActive()
                .stream()
                .map(this::mapToPublicDto)
                .toList();
        return ResponseEntity.ok(publicIncidents);
    }

    private PublicIncidentResponseDto mapToPublicDto(com.acousticguard.hub.incident.model.Incident incident) {
        Point locationGeo = incident.getLocationGeo();
        Float latitude = locationGeo != null ? obfuscateCoordinate((float) locationGeo.getY()) : null;
        Float longitude = locationGeo != null ? obfuscateCoordinate((float) locationGeo.getX()) : null;

        return new PublicIncidentResponseDto(
                incident.getId(),
                parseThreatType(incident.getType()),
                incident.getIntensity(),
                parseIncidentStatus(incident.getStatus()),
                incident.getCreatedAt(),
                latitude,
                longitude
        );
    }

    private Float obfuscateCoordinate(float coordinate) {
        return Math.round(coordinate * 100.0f) / 100.0f;
    }

    private com.acousticguard.hub.common.enums.ThreatType parseThreatType(String type) {
        if (type == null) {
            return com.acousticguard.hub.common.enums.ThreatType.BACKGROUND;
        }
        try {
            return com.acousticguard.hub.common.enums.ThreatType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return com.acousticguard.hub.common.enums.ThreatType.BACKGROUND;
        }
    }

    private com.acousticguard.hub.common.enums.IncidentStatus parseIncidentStatus(String status) {
        if (status == null) {
            return com.acousticguard.hub.common.enums.IncidentStatus.DETECTED;
        }
        try {
            return com.acousticguard.hub.common.enums.IncidentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return com.acousticguard.hub.common.enums.IncidentStatus.DETECTED;
        }
    }
}
