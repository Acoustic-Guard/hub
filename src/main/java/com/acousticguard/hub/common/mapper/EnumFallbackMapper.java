package com.acousticguard.hub.common.mapper;

import com.acousticguard.hub.common.enums.IncidentStatus;
import com.acousticguard.hub.common.enums.ThreatType;
import org.springframework.stereotype.Component;

/**
 * Component for mapping string values to enums with fallback defaults.
 * <p>
 * Provides safe conversion of string values to enum types, with fallback to
 * default values when the string does not match any enum constant. This is
 * used to handle enum mapping from external sources (e.g., database, API)
 * where invalid values may be encountered.
 * </p>
 */
@Component
public class EnumFallbackMapper {

    /**
     * Maps a string value to a ThreatType enum with fallback.
     * <p>
     * Converts the string to uppercase and attempts to match it to a ThreatType
     * constant. If the value is null, blank, or invalid, returns BACKGROUND as
     * a safe default.
     * </p>
     *
     * @param value the string value to map
     * @return the ThreatType enum value, or BACKGROUND if invalid
     */
    public ThreatType mapThreatType(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return ThreatType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ThreatType.BACKGROUND;
        }
    }

    /**
     * Maps a string value to an IncidentStatus enum with fallback.
     * <p>
     * Converts the string to uppercase and attempts to match it to an IncidentStatus
     * constant. If the value is null, blank, or invalid, returns DETECTED as
     * a safe default.
     * </p>
     *
     * @param value the string value to map
     * @return the IncidentStatus enum value, or DETECTED if invalid
     */
    public IncidentStatus mapIncidentStatus(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return IncidentStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return IncidentStatus.DETECTED;
        }
    }
}