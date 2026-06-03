package com.bragdev.frauddetection.common.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTransactionRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Merchant ID is required")
    private UUID merchantId;

    @NotNull(message = "Device ID is required")
    private UUID deviceId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be ISO 4217 (3 characters)")
    private String currency;

    private String ipAddress;

    @Size(min = 2, max = 2, message = "Country must be ISO 3166-1 alpha-2 (2 characters)")
    private String country;

    @NotNull(message = "Idempotency key is required")
    private String idempotencyKey;
}
