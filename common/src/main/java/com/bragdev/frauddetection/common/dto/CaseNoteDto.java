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
public class CaseNoteDto {

    private UUID id;

    private UUID caseId;

    private UUID authorId;

    private String content;

    private Instant createdAt;
}
