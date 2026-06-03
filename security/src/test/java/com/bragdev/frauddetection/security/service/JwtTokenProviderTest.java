package com.bragdev.frauddetection.security.service;

import com.bragdev.frauddetection.common.enums.Role;
import com.bragdev.frauddetection.security.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String EMAIL = "test@example.com";
    private static final Set<Role> ROLES = Set.of(Role.ADMIN, Role.ANALYST);

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties(
                "test-secret-key-must-be-at-least-32-bytes-long-for-hmac",
                java.time.Duration.ofMinutes(15),
                java.time.Duration.ofDays(7),
                "fraud-detection-system",
                "fraud-detection-api"
        );
        tokenProvider = new JwtTokenProvider(props);
    }

    @Test
    void generatesAndValidatesAccessToken() {
        String token = tokenProvider.generateAccessToken(USER_ID, EMAIL, ROLES);

        assertThat(token).isNotBlank();
        assertThat(tokenProvider.extractUserId(token)).isEqualTo(USER_ID);
        assertThat(tokenProvider.extractRoles(token)).containsExactlyInAnyOrder(Role.ADMIN, Role.ANALYST);
    }

    @Test
    void generatesAndValidatesRefreshToken() {
        String token = tokenProvider.generateRefreshToken(USER_ID);

        assertThat(token).isNotBlank();
        assertThat(tokenProvider.isRefreshToken(token)).isTrue();
        assertThat(tokenProvider.extractUserId(token)).isEqualTo(USER_ID);
    }

    @Test
    void accessTokenIsNotARefreshToken() {
        String token = tokenProvider.generateAccessToken(USER_ID, EMAIL, ROLES);

        assertThat(tokenProvider.isRefreshToken(token)).isFalse();
    }

    @Test
    void rejectsTamperedToken() {
        String token = tokenProvider.generateAccessToken(USER_ID, EMAIL, ROLES);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThatThrownBy(() -> tokenProvider.extractUserId(tampered))
                .isInstanceOf(Exception.class);
    }

    @Test
    void rejectsTokenWithWrongSecret() {
        JwtProperties otherProps = new JwtProperties(
                "different-secret-key-must-be-at-least-32-bytes",
                java.time.Duration.ofMinutes(15),
                java.time.Duration.ofDays(7),
                "fraud-detection-system",
                "fraud-detection-api"
        );
        JwtTokenProvider otherProvider = new JwtTokenProvider(otherProps);

        String token = tokenProvider.generateAccessToken(USER_ID, EMAIL, ROLES);

        assertThatThrownBy(() -> otherProvider.extractUserId(token))
                .isInstanceOf(Exception.class);
    }
}
