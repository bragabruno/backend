package com.bragdev.frauddetection.common.model;

import com.bragdev.frauddetection.common.enums.Decision;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "risk_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "model_version_id")
    private UUID modelVersionId;

    @Column(name = "ml_score", nullable = false)
    private double mlScore;

    @Column(name = "rules_score", nullable = false)
    private double rulesScore;

    @Column(name = "aggregate_score", nullable = false)
    private double aggregateScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Decision decision;

    @Column(name = "degraded_mode", nullable = false)
    private boolean degradedMode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reason_codes", columnDefinition = "jsonb")
    private List<String> reasonCodes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
