package com.acousticguard.hub.analytics.controller;

import com.acousticguard.hub.analytics.dto.AnalyticsResponseDto;
import com.acousticguard.hub.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    public ResponseEntity<AnalyticsResponseDto> getAnalytics(
            @RequestParam(defaultValue = "24h") String range,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        AnalyticsResponseDto analytics = analyticsService.getAnalytics(range, start, end);
        return ResponseEntity.ok(analytics);
    }
}
