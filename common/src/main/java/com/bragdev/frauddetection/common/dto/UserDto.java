package com.bragdev.frauddetection.common.dto;

import com.bragdev.frauddetection.common.enums.Role;
import com.bragdev.frauddetection.common.enums.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserDto(
    UUID id,
    String username,
    String email,
    Role role,
    UserStatus status,
    Instant createdAt
) {}
