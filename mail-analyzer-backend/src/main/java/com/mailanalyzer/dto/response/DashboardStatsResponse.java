package com.mailanalyzer.dto.response;

import com.mailanalyzer.enums.EmailCategory;
import com.mailanalyzer.enums.EmailPriority;

import java.util.Map;

/**
 * Response DTO for GET /api/emails/stats.
 * Provides data for the dashboard pie chart and bar chart.
 */
public record DashboardStatsResponse(
        long totalEmails,
        Map<EmailCategory, Long> countByCategory,   // for pie chart
        Map<EmailPriority, Long> countByPriority,   // for bar chart
        long connectedAccountsCount
) {}
