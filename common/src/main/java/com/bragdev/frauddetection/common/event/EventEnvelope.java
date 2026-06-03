package com.bragdev.frauddetection.common.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

/**
 * Versioned event envelope wrapping all domain events published to Kafka.
 * Provides a stable contract with id, type, version, correlation id, and timestamp.
 *
 * @param <T> the event payload type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        int version,
        String correlationId,
        Instant occurredAt,
        T payload
) {
    public static <T> EventEnvelope<T> of(String eventType, T payload) {
        return new EventEnvelope<>(
                UUID.randomUUID(),
                eventType,
                1,
                null,
                Instant.now(),
                payload
        );
    }

    public static <T> EventEnvelope<T> of(String eventType, T payload, String correlationId) {
        return new EventEnvelope<>(
                UUID.randomUUID(),
                eventType,
                1,
                correlationId,
                Instant.now(),
                payload
        );
    }
}
