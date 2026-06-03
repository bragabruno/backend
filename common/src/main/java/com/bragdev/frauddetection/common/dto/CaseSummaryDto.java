package com.bragdev.frauddetection.common.dto;

import com.bragdev.frauddetection.common.enums.CaseStatus;
import com.bragdev.frauddetection.common.enums.Severity;
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
public class CaseSummaryDto {

    private UUID id;

    private UUID transactionId;

    private UUID riskScoreId;

    private UUID assigneeId;

    private CaseStatus status;

    private Severity severity;

    private Instant openedAt;

    private Instant slaDueAt;

    private Instant resolvedAt;
}
