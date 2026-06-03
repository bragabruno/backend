package com.bragdev.frauddetection.common.model;

import com.bragdev.frauddetection.common.enums.LabelType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fraud_labels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudLabel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "case_id")
    private UUID caseId;

    @Column(name = "analyst_id", nullable = false)
    private UUID analystId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LabelType label;

    @Column(nullable = false)
    private double confidence;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @CreationTimestamp
    @Column(name = "labeled_at", updatable = false)
    private Instant labeledAt;
}
