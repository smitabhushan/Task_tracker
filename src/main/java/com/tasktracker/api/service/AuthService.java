package com.tasktracker.api.service;

import com.tasktracker.api.dto.AuthDtos.AuthResponse;
import com.tasktracker.api.dto.AuthDtos.LoginRequest;
import com.tasktracker.api.dto.AuthDtos.RefreshRequest;
import com.tasktracker.api.dto.AuthDtos.RegisterRequest;
import com.tasktracker.api.dto.AuthDtos.UserResponse;
import com.tasktracker.api.entity.Organization;
import com.tasktracker.api.entity.RefreshToken;
import com.tasktracker.api.entity.Role;
import com.tasktracker.api.entity.User;
import com.tasktracker.api.exception.ApiException;
import com.tasktracker.api.repository.OrganizationRepository;
import com.tasktracker.api.repository.RefreshTokenRepository;
import com.tasktracker.api.repository.UserRepository;
import com.tasktracker.api.security.JwtProperties;
import com.tasktracker.api.security.JwtService;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final OrganizationRepository organizations;
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final SecureRandom random = new SecureRandom();

    public AuthService(OrganizationRepository organizations, UserRepository users, RefreshTokenRepository refreshTokens,
                       PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                       JwtService jwtService, JwtProperties jwtProperties) {
        this.organizations = organizations;
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (users.existsByEmail(request.email().toLowerCase())) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_EXISTS", "email is already registered");
        }
        Organization org = new Organization();
        org.setName(request.organizationName());
        organizations.save(org);

        User user = new User();
        user.setOrganization(org);
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.ADMIN);
        users.save(user);
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password()));
        User user = users.findByEmail(request.email().toLowerCase()).orElseThrow();
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String oldHash = TokenHashing.sha256(request.refreshToken());
        RefreshToken oldToken = refreshTokens.findByTokenHash(oldHash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "refresh token is invalid"));
        if (oldToken.getRevokedAt() != null || oldToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "refresh token is expired or revoked");
        }
        User user = oldToken.getUser();
        String newRefreshToken = randomToken();
        String newHash = TokenHashing.sha256(newRefreshToken);
        oldToken.setRevokedAt(Instant.now());
        oldToken.setReplacedByHash(newHash);
        refreshTokens.save(oldToken);
        saveRefreshToken(user, newHash);
        return new AuthResponse(jwtService.createAccessToken(user), newRefreshToken, toUserResponse(user));
    }

    private AuthResponse issueTokens(User user) {
        String refreshToken = randomToken();
        saveRefreshToken(user, TokenHashing.sha256(refreshToken));
        return new AuthResponse(jwtService.createAccessToken(user), refreshToken, toUserResponse(user));
    }

    private void saveRefreshToken(User user, String hash) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hash);
        refreshToken.setExpiresAt(Instant.now().plusSeconds(jwtProperties.refreshTokenDays() * 24 * 60 * 60));
        refreshTokens.save(refreshToken);
    }

    private String randomToken() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getOrganizationId(), user.getName(), user.getEmail(), user.getRole());
    }
}
