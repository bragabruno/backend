package com.bragdev.frauddetection;

import com.bragdev.frauddetection.common.enums.Decision;
import com.bragdev.frauddetection.common.event.TransactionEvent;
import com.bragdev.frauddetection.common.model.RiskScore;
import com.bragdev.frauddetection.common.repository.FraudCaseRepository;
import com.bragdev.frauddetection.common.repository.RiskScoreRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end real-time scoring path (FRAUD-088) with the ml-service in the loop:
 * Kafka transaction event → fraud-engine consumer → feature extraction → rules + ML → decision →
 * persisted {@link RiskScore} + opened case → {@code fraud.scored} event.
 *
 * <p>Unlike {@link ScoringPipelineIT} (which exercises the degraded, ml-down path), this test points
 * the shared stub ml-service at a successful {@code /predict} response so the full non-degraded
 * rules+ML aggregation runs. With amount 75 000 (rules score 0.6) and an ML probability of 0.9, the
 * aggregate is {@code 0.6*0.9 + 0.4*0.6 = 0.78}, crossing the DECLINE threshold (0.7). The test also
 * asserts the ml-service received a well-formed snake_case request and that the terminal
 * {@code fraud.scored} event is published.
 */
class RealtimeScoringE2EIT extends IntegrationTest {

    private static final String PREDICT_RESPONSE = """
            {"transaction_id":"00000000-0000-0000-0000-000000000000",\
            "fraud_probability":0.9,"risk_level":"HIGH","model_version":"xgb-e2e-test",\
            "contributing_factors":["HIGH_AMOUNT","NEW_DEVICE"]}""";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private RiskScoreRepository riskScoreRepository;

    @Autowired
    private FraudCaseRepository fraudCaseRepository;

    @BeforeEach
    void mlUp() {
        mlServiceRespondsWith(PREDICT_RESPONSE);
    }

    @Test
    void scoresWithMlInLoopDeclinesPersistsAndEmitsFraudScored() {
        UUID transactionId = UUID.randomUUID();
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

        try (KafkaConsumer<String, String> scoredConsumer = scoredConsumer()) {
            kafkaTemplate.send("transaction-events", transactionId.toString(), event).join();

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                Optional<RiskScore> riskScore = riskScoreRepository.findByTransactionId(transactionId);
                assertThat(riskScore).isPresent();
                RiskScore score = riskScore.get();
                // ml-service is up -> full rules + ML path (not degraded).
                assertThat(score.isDegradedMode()).isFalse();
                assertThat(score.getMlScore()).isEqualTo(0.9);
                assertThat(score.getRulesScore()).isEqualTo(0.6);
                assertThat(score.getAggregateScore()).isCloseTo(0.78, within(1e-9));
                assertThat(score.getDecision()).isEqualTo(Decision.DECLINE);
                assertThat(score.getReasonCodes()).contains("ML_HIGH_AMOUNT", "ML_NEW_DEVICE");
                // DECLINE opens a case.
                assertThat(fraudCaseRepository.findByTransactionId(transactionId)).isPresent();
            });

            // The ml-service received the snake_case feature payload the FastAPI contract expects.
            assertThat(lastMlPredictRequest())
                    .isNotNull()
                    .contains("\"transaction_id\":\"" + transactionId + "\"")
                    .contains("\"amount\":75000")
                    .contains("\"country\":\"BR\"")
                    .contains("\"currency\":\"USD\"")
                    .contains("\"new_device\":");

            // The terminal fraud.scored event is published with the final decision.
            String scoredPayload = awaitScoredEvent(scoredConsumer, transactionId);
            assertThat(scoredPayload)
                    .contains("\"decision\":\"DECLINE\"")
                    .contains("\"degradedMode\":false");
        }
    }

    private KafkaConsumer<String, String> scoredConsumer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "e2e-fraud-scored-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of("fraud.scored"));
        return consumer;
    }

    private static String awaitScoredEvent(KafkaConsumer<String, String> consumer, UUID transactionId) {
        AtomicReference<String> payload = new AtomicReference<>();
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> record : records) {
                if (transactionId.toString().equals(record.key())) {
                    payload.set(record.value());
                }
            }
            assertThat(payload.get()).isNotNull();
        });
        return payload.get();
    }
}
