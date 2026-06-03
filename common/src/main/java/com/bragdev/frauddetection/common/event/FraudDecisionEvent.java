package com.bragdev.frauddetection.common.event;

import com.bragdev.frauddetection.common.enums.Decision;

import java.util.List;
import java.util.UUID;

/**
 * Immutable event published to the {@code fraud.scored} topic once the fraud-engine has
 * scored a transaction. Downstream consumers (analyst console SSE feed, analytics) react to
 * this without coupling to the scoring internals.
 */
public record FraudDecisionEvent(
        UUID transactionId,
        UUID riskScoreId,
        UUID caseId,
        Decision decision,
        double aggregateScore,
        boolean degradedMode,
        List<String> reasonCodes
) {
}
