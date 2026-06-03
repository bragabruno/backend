package com.bragdev.frauddetection.common.event;

import com.bragdev.frauddetection.common.model.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable event published to the {@code transaction-events} topic after a transaction is
 * persisted. Carries the full transaction snapshot so the fraud-engine consumer can score
 * without reading back from the database (removing the publish-before-commit race that a
 * bare id would introduce).
 */
public record TransactionEvent(
        UUID transactionId,
        UUID userId,
        UUID merchantId,
        UUID deviceId,
        BigDecimal amount,
        String currency,
        String ipAddress,
        String country,
        Instant occurredAt
) {

    public static TransactionEvent from(Transaction transaction) {
        return new TransactionEvent(
                transaction.getId(),
                transaction.getUserId(),
                transaction.getMerchantId(),
                transaction.getDeviceId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getIpAddress(),
                transaction.getCountry(),
                transaction.getCreatedAt()
        );
    }

    /** Reconstructs a transient {@link Transaction} for scoring (not attached to any persistence context). */
    public Transaction toTransaction() {
        return Transaction.builder()
                .id(transactionId)
                .userId(userId)
                .merchantId(merchantId)
                .deviceId(deviceId)
                .amount(amount)
                .currency(currency)
                .ipAddress(ipAddress)
                .country(country)
                .build();
    }
}
