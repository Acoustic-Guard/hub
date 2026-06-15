package com.acousticguard.hub.analytics.controller;

import com.acousticguard.hub.analytics.dto.AnalyticsResponseDto;
import com.acousticguard.hub.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Analytics and statistics endpoints")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    @Operation(summary = "Get analytics data", description = "Retrieve analytics statistics for incidents and alerts")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved analytics data")
    })
    public ResponseEntity<AnalyticsResponseDto> getAnalytics(
            @Parameter(description = "Time range (e.g., 24h, 7d)")
            @RequestParam(defaultValue = "24h") String range,
            @Parameter(description = "Start timestamp (ISO format)")
            @RequestParam(required = false) String start,
            @Parameter(description = "End timestamp (ISO format)")
            @RequestParam(required = false) String end) {
        AnalyticsResponseDto analytics = analyticsService.getAnalytics(range, start, end);
        return ResponseEntity.ok(analytics);
    }
}
