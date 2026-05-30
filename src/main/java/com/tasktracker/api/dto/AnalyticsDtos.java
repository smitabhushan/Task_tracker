package com.tasktracker.api.dto;

import java.util.List;

public final class AnalyticsDtos {
    private AnalyticsDtos() {
    }

    public record OverdueByUser(Long userId, String name, long overdueCount) {
    }

    public record AnalyticsResponse(List<OverdueByUser> overdueByUser, Double averageCompletionHours) {
    }
}
