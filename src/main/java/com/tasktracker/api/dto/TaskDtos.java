package com.tasktracker.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.tasktracker.api.entity.Priority;
import com.tasktracker.api.entity.TaskStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

public final class TaskDtos {
    private TaskDtos() {
    }

    public record CreateTaskRequest(
            @NotNull Long projectId,
            @NotNull Long assigneeId,
            @NotBlank @Size(max = 220) String title,
            @Size(max = 4000) String description,
            @NotNull Priority priority,
            @JsonAlias("dueDate")
            @JsonProperty("due_date")
            @FutureOrPresent(message = "due_date must be a future or present date") LocalDate dueDate
    ) {
    }

    public record UpdateTaskRequest(
            @NotNull Long assigneeId,
            @NotBlank @Size(max = 220) String title,
            @Size(max = 4000) String description,
            @NotNull Priority priority,
            @JsonAlias("dueDate")
            @JsonProperty("due_date")
            @FutureOrPresent(message = "due_date must be a future or present date") LocalDate dueDate
    ) {
    }

    public record ChangeStatusRequest(@NotNull TaskStatus status) {
    }

    public record TaskResponse(
            Long id,
            Long projectId,
            Long assigneeId,
            String assigneeName,
            String title,
            String description,
            Priority priority,
            TaskStatus status,
            @JsonProperty("due_date")
            LocalDate dueDate,
            @JsonProperty("completed_at")
            Instant completedAt,
            @JsonProperty("created_at")
            Instant createdAt,
            @JsonProperty("updated_at")
            Instant updatedAt
    ) {
    }
}
