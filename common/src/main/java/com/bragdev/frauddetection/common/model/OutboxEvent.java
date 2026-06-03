package com.bragdev.frauddetection.common.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbox event entity for the transactional outbox pattern.
 * Events are persisted in the same DB transaction as the domain change,
 * then relayed to Kafka by a background poller. Guarantees at-least-once delivery.
 */
@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String topic;

    @Column(name = "event_key", nullable = false)
    private String eventKey;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(nullable = false)
    @Builder.Default
    private int attempts = 0;

    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private int maxAttempts = 3;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    public void markSent() {
        this.status = "SENT";
        this.sentAt = Instant.now();
    }

    public void markFailed(String error) {
        this.attempts++;
        this.lastError = error;
        if (this.attempts >= this.maxAttempts) {
            this.status = "DEAD";
        }
    }

    public boolean isPending() {
        return "PENDING".equals(this.status);
    }

    public boolean isDead() {
        return "DEAD".equals(this.status);
    }
}
