package com.tasktracker.api.service;

import com.tasktracker.api.dto.AnalyticsDtos.AnalyticsResponse;
import com.tasktracker.api.dto.AnalyticsDtos.OverdueByUser;
import com.tasktracker.api.repository.TaskRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {
    private final TaskRepository tasks;
    private final CurrentUser currentUser;

    public AnalyticsService(TaskRepository tasks, CurrentUser currentUser) {
        this.tasks = tasks;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse summary() {
        Long orgId = currentUser.get().getOrganizationId();
        var overdue = tasks.overdueCountByUser(orgId, LocalDate.now()).stream()
                .map(row -> new OverdueByUser((Long) row[0], (String) row[1], ((Number) row[2]).longValue()))
                .toList();
        Double seconds = tasks.averageCompletionSeconds(orgId);
        return new AnalyticsResponse(overdue, seconds == null ? null : seconds / 3600.0);
    }
}
