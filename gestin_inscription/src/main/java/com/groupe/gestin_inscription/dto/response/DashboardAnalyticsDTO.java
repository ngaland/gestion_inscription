package com.groupe.gestin_inscription.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardAnalyticsDTO {
    // 1. Total Counts
    private Long totalApplications;
    private Long preValidationCount;
    private Long approvedCount;
    private Long rejectedCount;

    // 2. Counts by Status (More detailed breakdown)
    // Map<Status_Name, Count> e.g., {"PENDING_RECOURSE": 5, "MANUAL_REVIEW": 15}
    private Map<String, Long> applicationsByStatus;

    // 3. Completion rate by step (Assuming this is a separate metric)
    // Map<Step_Name, Percentage_or_Count>
    private Map<String, Long> completionRateByStep;

    private Map<String, Long> registrationHeatmapData;



    // Getters and Setters
}