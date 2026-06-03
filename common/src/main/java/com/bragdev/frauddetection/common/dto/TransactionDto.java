package com.bragdev.frauddetection.common.dto;

import com.bragdev.frauddetection.common.enums.TransactionStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDto {

    private UUID id;

    private UUID userId;

    private UUID merchantId;

    private UUID deviceId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be ISO 4217 (3 characters)")
    private String currency;

    private String ipAddress;

    @Size(min = 2, max = 2, message = "Country must be ISO 3166-1 alpha-2 (2 characters)")
    private String country;

    private TransactionStatus status;

    private String idempotencyKey;

    private Instant createdAt;

    private RiskScoreDto latestRiskScore;
}
