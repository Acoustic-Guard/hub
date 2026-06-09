package com.acousticguard.hub.analytics.service;

import com.acousticguard.hub.analytics.dto.AnalyticsResponseDto;

public interface AnalyticsService {
    AnalyticsResponseDto getAnalytics(String range, String start, String end);
}
