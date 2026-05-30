package com.tasktracker.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ProjectDtos {
    private ProjectDtos() {
    }

    public record ProjectRequest(@NotBlank @Size(max = 180) String name, @Size(max = 2000) String description) {
    }

    public record ProjectResponse(Long id, String name, String description) {
    }
}
