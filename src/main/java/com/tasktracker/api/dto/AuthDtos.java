package com.tasktracker.api.dto;

import com.tasktracker.api.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 160) String organizationName,
            @NotBlank @Size(max = 140) String name,
            @Email @NotBlank @Size(max = 190) String email,
            @NotBlank @Size(min = 8, max = 100) String password
    ) {
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record AuthResponse(String accessToken, String refreshToken, UserResponse user) {
    }

    public record UserResponse(Long id, Long organizationId, String name, String email, Role role) {
    }
}
