package com.bragdev.frauddetection.casemanagement.service;

import com.bragdev.frauddetection.common.enums.Severity;
import com.bragdev.frauddetection.common.model.FraudCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Computes a priority score for fraud cases in the review queue.
 * Combines risk severity, SLA age, and transaction value into a single ranking signal.
 */
@Service
public class CasePrioritizationService {

    @Value("${fraud.case.priority.weight.severity:1.0}")
    private double severityWeight;

    @Value("${fraud.case.priority.weight.sla-age:0.5}")
    private double slaAgeWeight;

    @Value("${fraud.case.priority.weight.value:0.3}")
    private double valueWeight;

    public double computePriority(FraudCase fraudCase, double transactionValue) {
        double severityScore = severityScore(fraudCase.getSeverity());
        double slaAgeScore = slaAgeScore(fraudCase.getOpenedAt(), fraudCase.getSlaDueAt());
        double valueScore = valueScore(transactionValue);

        return (severityWeight * severityScore)
                + (slaAgeWeight * slaAgeScore)
                + (valueWeight * valueScore);
    }

    private double severityScore(Severity severity) {
        return switch (severity) {
            case CRITICAL -> 1.0;
            case HIGH -> 0.75;
            case MEDIUM -> 0.5;
            case LOW -> 0.25;
        };
    }

    private double slaAgeScore(Instant openedAt, Instant slaDueAt) {
        if (openedAt == null || slaDueAt == null) {
            return 0.0;
        }
        Duration totalSla = Duration.between(openedAt, slaDueAt);
        Duration elapsed = Duration.between(openedAt, Instant.now());
        if (totalSla.isNegative() || totalSla.isZero()) {
            return 1.0;
        }
        double ratio = (double) elapsed.toMillis() / totalSla.toMillis();
        return Math.min(1.0, Math.max(0.0, ratio));
    }

    private double valueScore(double transactionValue) {
        // Normalize: $10k+ transactions get max priority
        return Math.min(1.0, transactionValue / 10_000.0);
    }
}
