package com.bragdev.frauddetection.casemanagement.controller;

import com.bragdev.frauddetection.casemanagement.service.CaseService;
import com.bragdev.frauddetection.casemanagement.sse.CaseEventPublisher;
import com.bragdev.frauddetection.common.dto.CaseDetailDto;
import com.bragdev.frauddetection.common.dto.CaseLabelDto;
import com.bragdev.frauddetection.common.dto.CaseNoteDto;
import com.bragdev.frauddetection.common.dto.CaseSummaryDto;
import com.bragdev.frauddetection.common.dto.CreateCaseLabelRequest;
import com.bragdev.frauddetection.common.dto.CreateCaseNoteRequest;
import com.bragdev.frauddetection.common.dto.PageResponse;
import com.bragdev.frauddetection.common.enums.CaseStatus;
import com.bragdev.frauddetection.common.enums.Severity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fraud-cases")
@Tag(name = "Case Management", description = "Fraud case lifecycle endpoints")
public class CaseController {

    private final CaseService caseService;
    private final CaseEventPublisher eventPublisher;

    public CaseController(CaseService caseService, CaseEventPublisher eventPublisher) {
        this.caseService = caseService;
        this.eventPublisher = eventPublisher;
    }

    @GetMapping
    @Operation(summary = "List cases (paginated, filterable, sorted by severity DESC then openedAt ASC)")
    public PageResponse<CaseSummaryDto> listCases(
            @RequestParam(required = false) CaseStatus status,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<CaseSummaryDto> result = caseService.listCases(
                status, severity, assigneeId,
                PageRequest.of(page, size));
        return PageResponse.of(result.getContent(), result.getNumber(),
                result.getSize(), result.getTotalElements());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get case detail with embedded transaction, risk score, notes, and labels")
    public CaseDetailDto getCase(@PathVariable UUID id) {
        return caseService.getCase(id);
    }

    @PutMapping("/{id}/assign")
    @Operation(summary = "Assign case to analyst (OPEN → ASSIGNED)")
    public CaseDetailDto assignCase(@PathVariable UUID id, @RequestBody AssignRequest body) {
        return caseService.assignCase(id, body.assigneeId());
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Transition case status (enforces state machine)")
    public CaseDetailDto updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateStatusRequest body) {
        return caseService.updateStatus(id, body.status());
    }

    @GetMapping("/{id}/notes")
    @Operation(summary = "List notes for a case, chronological")
    public List<CaseNoteDto> getNotes(@PathVariable UUID id) {
        return caseService.getNotes(id);
    }

    @PostMapping("/{id}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a note to a case")
    public CaseNoteDto addNote(
            @PathVariable UUID id,
            @Valid @RequestBody CreateCaseNoteRequest body,
            @AuthenticationPrincipal UUID currentUserId
    ) {
        return caseService.addNote(id, currentUserId, body);
    }

    @PostMapping("/{id}/labels")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit a fraud label (triggers case resolution and Kafka event)")
    public CaseLabelDto addLabel(
            @PathVariable UUID id,
            @Valid @RequestBody CreateCaseLabelRequest body,
            @AuthenticationPrincipal UUID currentUserId
    ) {
        return caseService.addLabel(id, currentUserId, body);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE stream for real-time case updates (30s heartbeat)")
    public SseEmitter streamCases() {
        return eventPublisher.createEmitter();
    }

    record AssignRequest(UUID assigneeId) {}
    record UpdateStatusRequest(@Valid CaseStatus status) {}
}
