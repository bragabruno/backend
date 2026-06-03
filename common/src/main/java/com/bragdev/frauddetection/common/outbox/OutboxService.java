package com.bragdev.frauddetection.common.outbox;

import com.bragdev.frauddetection.common.model.OutboxEvent;
import com.bragdev.frauddetection.common.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for persisting events to the outbox table within the same DB transaction as domain changes.
 * The {@link OutboxRelay} will pick up pending events and publish them to Kafka.
 */
@Service
public class OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void publish(String topic, String eventKey, String eventType, Object payload) {
        publish(topic, eventKey, eventType, payload, null);
    }

    @Transactional
    public void publish(String topic, String eventKey, String eventType, Object payload, String correlationId) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            OutboxEvent event = OutboxEvent.builder()
                    .topic(topic)
                    .eventKey(eventKey)
                    .eventType(eventType)
                    .payload(json)
                    .correlationId(correlationId)
                    .build();
            outboxRepository.save(event);
            log.debug("Outbox event saved: {} -> {}", topic, eventKey);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }
    }
}
