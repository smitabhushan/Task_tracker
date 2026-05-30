package com.tasktracker.api.dto;

import com.tasktracker.api.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class UserDtos {
    private UserDtos() {
    }

    public record CreateUserRequest(
            @NotBlank @Size(max = 140) String name,
            @Email @NotBlank @Size(max = 190) String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotNull Role role
    ) {
    }

    public record UpdateUserRoleRequest(@NotNull Role role, Boolean active) {
    }
}
