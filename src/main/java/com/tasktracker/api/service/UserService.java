package com.tasktracker.api.service;

import com.tasktracker.api.dto.AuthDtos.UserResponse;
import com.tasktracker.api.dto.UserDtos.CreateUserRequest;
import com.tasktracker.api.dto.UserDtos.UpdateUserRoleRequest;
import com.tasktracker.api.entity.User;
import com.tasktracker.api.exception.ApiException;
import com.tasktracker.api.repository.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository users;
    private final CurrentUser currentUser;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, CurrentUser currentUser, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.currentUser = currentUser;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return users.findByOrganization_IdAndActiveTrue(currentUser.get().getOrganizationId()).stream().map(AuthService::toUserResponse).toList();
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (users.existsByEmail(request.email().toLowerCase())) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_EXISTS", "email is already registered");
        }
        User admin = currentUser.get();
        User user = new User();
        user.setOrganization(admin.getOrganization());
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        users.save(user);
        return AuthService.toUserResponse(user);
    }

    @Transactional
    public UserResponse updateRole(Long id, UpdateUserRoleRequest request) {
        User user = users.findByIdAndOrganization_Id(id, currentUser.get().getOrganizationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "user not found"));
        user.setRole(request.role());
        if (request.active() != null) {
            user.setActive(request.active());
        }
        return AuthService.toUserResponse(user);
    }

    @Transactional
    public void delete(Long id) {
        User actor = currentUser.get();
        if (actor.getId().equals(id)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CANNOT_DELETE_SELF", "you cannot delete your own user");
        }
        User user = users.findByIdAndOrganization_Id(id, actor.getOrganizationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "user not found"));
        user.setActive(false);
    }
}
