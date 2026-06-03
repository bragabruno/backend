package com.bragdev.frauddetection.security.dto;

import java.time.Instant;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    Instant expiresAt
) {}
