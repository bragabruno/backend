package com.bragdev.frauddetection.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "fraud.security.jwt")
public record JwtProperties(
    String secret,
    Duration accessTtl,
    Duration refreshTtl,
    String issuer,
    String audience
) {
    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret must not be blank");
        }
    }
}
