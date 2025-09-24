package com.groupe.gestin_inscription.controller;

import com.groupe.gestin_inscription.dto.response.ApplicationStatusResponseDto;
import com.groupe.gestin_inscription.dto.response.DashboardAnalyticsDTO;
import com.groupe.gestin_inscription.services.serviceImpl.AnalyticsServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics", description = "Endpoints for the administrator dashboard")
public class AnalyticsController {

    @Autowired
    private AnalyticsServiceImpl analyticsService;

    @Operation(summary = "Get real-time statistics for the dashboard")
    @GetMapping("/stats")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<DashboardAnalyticsDTO> getRealTimeStatistics() {
        DashboardAnalyticsDTO stats = analyticsService.getRealTimeStatistics();
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "Get alerts for blocked applications")
    @GetMapping("/alerts")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<ApplicationStatusResponseDto>> getAlerts() {
        List<ApplicationStatusResponseDto> alerts = analyticsService.getAlerts();
        return ResponseEntity.ok(alerts);
    }
}
