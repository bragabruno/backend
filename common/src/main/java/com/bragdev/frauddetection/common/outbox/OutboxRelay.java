package com.bragdev.frauddetection.common.outbox;

import com.bragdev.frauddetection.common.model.OutboxEvent;
import com.bragdev.frauddetection.common.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Polls the outbox table for pending events and publishes them to Kafka.
 * Runs in the same process as the application (no CDC required).
 * Guarantees at-least-once delivery: events are only marked SENT after successful publish.
 */
@Service
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int BATCH_SIZE = 50;

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OutboxRelay(OutboxEventRepository outboxRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${fraud.outbox.poll-interval-ms:1000}")
    public void relayPendingEvents() {
        List<OutboxEvent> pending = outboxRepository.findPending(BATCH_SIZE);
        if (pending.isEmpty()) {
            return;
        }
        log.debug("Relaying {} outbox events", pending.size());
        for (OutboxEvent event : pending) {
            try {
                publishEvent(event);
                event.markSent();
                outboxRepository.save(event);
            } catch (Exception e) {
                log.error("Failed to relay outbox event {}: {}", event.getId(), e.getMessage());
                event.markFailed(e.getMessage());
                outboxRepository.save(event);
            }
        }
    }

    private void publishEvent(OutboxEvent event) {
        kafkaTemplate.send(event.getTopic(), event.getEventKey(), event.getPayload()).join();
    }
}
