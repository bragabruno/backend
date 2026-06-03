package com.bragdev.frauddetection.rules.spi;

import java.util.Map;

public record RuleOutcome(
    double score,
    String reasonCode,
    Map<String, Object> details
) {
    public static RuleOutcome clean() {
        return new RuleOutcome(0.0, "CLEAN", Map.of());
    }

    public static RuleOutcome flagged(double score, String reasonCode, Map<String, Object> details) {
        return new RuleOutcome(score, reasonCode, details);
    }
}
