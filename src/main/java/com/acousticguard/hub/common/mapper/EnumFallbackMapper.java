package com.acousticguard.hub.common.mapper;

import com.acousticguard.hub.common.enums.IncidentStatus;
import com.acousticguard.hub.common.enums.ThreatType;
import org.springframework.stereotype.Component;

@Component
public class EnumFallbackMapper {

    public ThreatType mapThreatType(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return ThreatType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ThreatType.BACKGROUND;
        }
    }

    public IncidentStatus mapIncidentStatus(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return IncidentStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return IncidentStatus.DETECTED;
        }
    }
}