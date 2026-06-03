package com.bragdev.frauddetection.security.service;

import com.bragdev.frauddetection.common.enums.Role;
import com.bragdev.frauddetection.common.enums.UserStatus;
import com.bragdev.frauddetection.common.model.User;
import com.bragdev.frauddetection.common.repository.UserRepository;
import com.bragdev.frauddetection.security.config.JwtProperties;
import com.bragdev.frauddetection.security.dto.AuthResponse;
import com.bragdev.frauddetection.security.dto.LoginRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;
    private final StringRedisTemplate redisTemplate;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            JwtProperties jwtProperties,
            StringRedisTemplate redisTemplate
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.jwtProperties = jwtProperties;
        this.redisTemplate = redisTemplate;
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String accessToken = tokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), Set.of(user.getRole())
        );
        String refreshToken = tokenProvider.generateRefreshToken(user.getId());

        return new AuthResponse(
                accessToken,
                refreshToken,
                Instant.now().plus(jwtProperties.accessTtl())
        );
    }

    public AuthResponse refresh(String refreshToken) {
        if (!tokenProvider.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        UUID userId = tokenProvider.extractUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active");
        }

        String newAccessToken = tokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), Set.of(user.getRole())
        );
        String newRefreshToken = tokenProvider.generateRefreshToken(user.getId());

        return new AuthResponse(
                newAccessToken,
                newRefreshToken,
                Instant.now().plus(jwtProperties.accessTtl())
        );
    }

    public void logout(String refreshToken) {
        try {
            UUID userId = tokenProvider.extractUserId(refreshToken);
            redisTemplate.opsForSet().add("revoked_tokens:" + userId, refreshToken);
            redisTemplate.expire("revoked_tokens:" + userId, jwtProperties.refreshTtl());
        } catch (Exception e) {
            // Token already invalid, ignore
        }
    }

    public boolean isTokenRevoked(String token) {
        try {
            UUID userId = tokenProvider.extractUserId(token);
            return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember("revoked_tokens:" + userId, token));
        } catch (Exception e) {
            return true;
        }
    }
}
