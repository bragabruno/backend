package com.bragdev.frauddetection.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseLabelDto {

    private UUID id;

    private UUID transactionId;

    private UUID caseId;

    private UUID analystId;

    private String label;

    private double confidence;

    private String reason;

    private Instant labeledAt;
}
