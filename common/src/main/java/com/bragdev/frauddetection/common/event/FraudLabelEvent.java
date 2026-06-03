package com.bragdev.frauddetection.common.event;

import java.util.UUID;

public record FraudLabelEvent(
        UUID transactionId,
        UUID caseId,
        UUID analystId,
        String label,
        double confidence
) {}
