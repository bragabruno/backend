package com.bragdev.frauddetection.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskScoreDto {

    private UUID id;

    private UUID transactionId;

    private UUID modelVersionId;

    private double mlScore;

    private double rulesScore;

    private double aggregateScore;

    private String decision;

    private boolean degradedMode;

    private List<String> reasonCodes;

    private Instant createdAt;
}
