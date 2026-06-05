package com.bragdev.frauddetection;

import com.bragdev.frauddetection.common.enums.Decision;
import com.bragdev.frauddetection.common.event.TransactionEvent;
import com.bragdev.frauddetection.common.model.RiskScore;
import com.bragdev.frauddetection.common.repository.FraudCaseRepository;
import com.bragdev.frauddetection.common.repository.RiskScoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Full event-driven path: publish a transaction event to Kafka and assert the fraud-engine consumer
 * scores it and persists a {@link RiskScore} (and opens a case for the REVIEW outcome). The stub
 * ml-service is forced down (503), so the {@link com.bragdev.frauddetection.rules.service.MlPredictionClient}
 * circuit breaker trips and scoring proceeds rules-only in degraded mode — exercising the resilience
 * path end to end. See {@link RealtimeScoringE2EIT} for the non-degraded rules+ML path.
 */
class ScoringPipelineIT extends IntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void mlDown() {
        mlServiceDown();
    }

    @Autowired
    private RiskScoreRepository riskScoreRepository;

    @Autowired
    private FraudCaseRepository fraudCaseRepository;

    @Test
    void scoresTransactionFromKafkaAndPersistsRiskScoreAndCase() {
        UUID transactionId = UUID.randomUUID();
        // Amount above the VERY_HIGH_AMOUNT rule threshold (50k) -> rules score 0.6 -> REVIEW.
        TransactionEvent event = new TransactionEvent(
                transactionId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("75000.00"),
                "USD",
                "10.0.0.1",
                "BR",
                Instant.parse("2026-06-03T00:00:00Z"));

        kafkaTemplate.send("transactions.created", transactionId.toString(), event).join();

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            Optional<RiskScore> riskScore = riskScoreRepository.findByTransactionId(transactionId);
            assertThat(riskScore).isPresent();
            assertThat(riskScore.get().getDecision()).isEqualTo(Decision.REVIEW);
            // ml-service is down -> degraded (rules-only) scoring.
            assertThat(riskScore.get().isDegradedMode()).isTrue();
            assertThat(riskScore.get().getAggregateScore()).isEqualTo(0.6);
            assertThat(fraudCaseRepository.findByTransactionId(transactionId)).isPresent();
        });
    }
}
