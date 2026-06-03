package com.bragdev.frauddetection.security.service;

import com.bragdev.frauddetection.common.enums.Role;
import com.bragdev.frauddetection.security.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("JavaUtilDate")
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId, String email, Set<Role> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.accessTtl().toMillis());

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roles.stream().map(Enum::name).collect(Collectors.toSet()))
                .issuer(jwtProperties.issuer())
                .audience().add(jwtProperties.audience()).and()
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(UUID userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.refreshTtl().toMillis());

        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "refresh")
                .issuer(jwtProperties.issuer())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(jwtProperties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isRefreshToken(String token) {
        try {
            Claims claims = validateToken(token);
            return "refresh".equals(claims.get("type", String.class));
        } catch (JwtException e) {
            return false;
        }
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(validateToken(token).getSubject());
    }

    public Set<Role> extractRoles(String token) {
        // jjwt deserializes the JSON array claim as a List, so it must be read as List (not Set),
        // otherwise get(..., Set.class) throws RequiredTypeException.
        @SuppressWarnings("unchecked")
        List<String> roleNames = validateToken(token).get("roles", List.class);
        if (roleNames == null) {
            return Set.of();
        }
        return roleNames.stream()
                .map(Role::valueOf)
                .collect(Collectors.toSet());
    }
}
