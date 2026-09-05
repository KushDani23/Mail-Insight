package com.mailinsight.dto.response;

import com.mailinsight.enums.EmailCategory;
import com.mailinsight.enums.EmailPriority;

import java.util.Map;

public record DashboardStatsResponse(long totalEmails, Map<EmailCategory, Long> countByCategory,
                Map<EmailPriority, Long> countByPriority, long connectedAccountsCount) {
}
