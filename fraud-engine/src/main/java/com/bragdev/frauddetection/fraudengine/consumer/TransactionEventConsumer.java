package com.bragdev.frauddetection.fraudengine.consumer;

import com.bragdev.frauddetection.common.event.TransactionEvent;
import com.bragdev.frauddetection.fraudengine.service.FraudScoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code transaction-events} and drives the fraud-scoring pipeline. This is the
 * event-driven entry point of the Fraud Detection Service: the {@code transactions} module
 * publishes here after a transaction is persisted, and scoring happens asynchronously.
 */
@Component
public class TransactionEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventConsumer.class);

    private final FraudScoringService fraudScoringService;

    public TransactionEventConsumer(FraudScoringService fraudScoringService) {
        this.fraudScoringService = fraudScoringService;
    }

    @KafkaListener(topics = "transaction-events", groupId = "${spring.kafka.consumer.group-id:fraud-detection-group}")
    public void onTransactionEvent(TransactionEvent event) {
        log.info("Scoring transaction {}", event.transactionId());
        fraudScoringService.score(event);
    }
}
