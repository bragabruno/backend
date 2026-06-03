package com.bragdev.frauddetection.casemanagement.service;

import com.bragdev.frauddetection.casemanagement.sse.CaseEventPublisher;
import com.bragdev.frauddetection.common.config.ResourceNotFoundException;
import com.bragdev.frauddetection.common.dto.CaseDetailDto;
import com.bragdev.frauddetection.common.dto.CaseLabelDto;
import com.bragdev.frauddetection.common.dto.CaseNoteDto;
import com.bragdev.frauddetection.common.dto.CaseSummaryDto;
import com.bragdev.frauddetection.common.dto.CreateCaseLabelRequest;
import com.bragdev.frauddetection.common.dto.CreateCaseNoteRequest;
import com.bragdev.frauddetection.common.enums.CaseStatus;
import com.bragdev.frauddetection.common.enums.LabelType;
import com.bragdev.frauddetection.common.enums.Severity;
import com.bragdev.frauddetection.common.event.FraudLabelEvent;
import com.bragdev.frauddetection.common.mapper.CaseNoteMapper;
import com.bragdev.frauddetection.common.mapper.FraudCaseMapper;
import com.bragdev.frauddetection.common.mapper.FraudLabelMapper;
import com.bragdev.frauddetection.common.mapper.RiskScoreMapper;
import com.bragdev.frauddetection.common.mapper.TransactionMapper;
import com.bragdev.frauddetection.common.model.AuditEvent;
import com.bragdev.frauddetection.common.model.CaseNote;
import com.bragdev.frauddetection.common.model.FraudCase;
import com.bragdev.frauddetection.common.model.FraudLabel;
import com.bragdev.frauddetection.common.repository.AuditEventRepository;
import com.bragdev.frauddetection.common.repository.CaseNoteRepository;
import com.bragdev.frauddetection.common.repository.FraudCaseRepository;
import com.bragdev.frauddetection.common.repository.FraudLabelRepository;
import com.bragdev.frauddetection.common.repository.RiskScoreRepository;
import com.bragdev.frauddetection.common.repository.TransactionRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class CaseService {

    private static final Logger log = LoggerFactory.getLogger(CaseService.class);
    private static final long SLA_HOURS = 24;
    private static final String TOPIC_FRAUD_CONFIRMED = "fraud.confirmed";
    private static final String TOPIC_FRAUD_FALSE_POSITIVE = "fraud.falsepositive";

    private final FraudCaseRepository caseRepository;
    private final AuditEventRepository auditEventRepository;
    private final CaseNoteRepository caseNoteRepository;
    private final FraudLabelRepository fraudLabelRepository;
    private final TransactionRepository transactionRepository;
    private final RiskScoreRepository riskScoreRepository;
    private final FraudCaseMapper caseMapper;
    private final CaseNoteMapper caseNoteMapper;
    private final FraudLabelMapper fraudLabelMapper;
    private final TransactionMapper transactionMapper;
    private final RiskScoreMapper riskScoreMapper;
    private final CaseEventPublisher eventPublisher;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @SuppressWarnings("ParameterNumber")
    public CaseService(
            FraudCaseRepository caseRepository,
            AuditEventRepository auditEventRepository,
            CaseNoteRepository caseNoteRepository,
            FraudLabelRepository fraudLabelRepository,
            TransactionRepository transactionRepository,
            RiskScoreRepository riskScoreRepository,
            FraudCaseMapper caseMapper,
            CaseNoteMapper caseNoteMapper,
            FraudLabelMapper fraudLabelMapper,
            TransactionMapper transactionMapper,
            RiskScoreMapper riskScoreMapper,
            CaseEventPublisher eventPublisher,
            KafkaTemplate<String, Object> kafkaTemplate,
            MeterRegistry meterRegistry
    ) {
        this.caseRepository = caseRepository;
        this.auditEventRepository = auditEventRepository;
        this.caseNoteRepository = caseNoteRepository;
        this.fraudLabelRepository = fraudLabelRepository;
        this.transactionRepository = transactionRepository;
        this.riskScoreRepository = riskScoreRepository;
        this.caseMapper = caseMapper;
        this.caseNoteMapper = caseNoteMapper;
        this.fraudLabelMapper = fraudLabelMapper;
        this.transactionMapper = transactionMapper;
        this.riskScoreMapper = riskScoreMapper;
        this.eventPublisher = eventPublisher;
        this.kafkaTemplate = kafkaTemplate;

        Gauge.builder("cases.open.count", caseRepository, r -> r.countByStatus(CaseStatus.OPEN))
                .description("Number of fraud cases currently in OPEN status")
                .register(meterRegistry);
    }

    @Transactional
    public CaseDetailDto createCase(UUID transactionId, UUID riskScoreId, Severity severity) {
        return caseRepository.findByTransactionId(transactionId)
                .map(caseMapper::toDetailDto)
                .orElseGet(() -> {
                    FraudCase fraudCase = FraudCase.builder()
                            .transactionId(transactionId)
                            .riskScoreId(riskScoreId)
                            .status(CaseStatus.OPEN)
                            .severity(severity)
                            .slaDueAt(Instant.now().plus(SLA_HOURS, ChronoUnit.HOURS))
                            .build();

                    FraudCase saved = caseRepository.save(fraudCase);
                    writeAudit(saved, "CASE_CREATED", "Case auto-created for transaction " + transactionId);
                    eventPublisher.publishCaseCreated(saved);
                    log.info("Case created for transaction {}: {}", transactionId, saved.getId());
                    return caseMapper.toDetailDto(saved);
                });
    }

    @Transactional(readOnly = true)
    public Page<CaseSummaryDto> listCases(CaseStatus status, Severity severity, UUID assigneeId, Pageable pageable) {
        return caseRepository.findByFilters(status, severity, assigneeId, pageable)
                .map(caseMapper::toSummaryDto);
    }

    @Transactional(readOnly = true)
    public CaseDetailDto getCase(UUID id) {
        FraudCase fraudCase = caseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + id));

        CaseDetailDto dto = caseMapper.toDetailDto(fraudCase);

        transactionRepository.findById(fraudCase.getTransactionId())
                .ifPresent(t -> dto.setTransaction(transactionMapper.toDto(t)));

        if (fraudCase.getRiskScoreId() != null) {
            riskScoreRepository.findById(fraudCase.getRiskScoreId())
                    .ifPresent(rs -> dto.setRiskScore(riskScoreMapper.toDto(rs)));
        }

        dto.setNotes(caseNoteRepository.findByCaseIdOrderByCreatedAtAsc(id)
                .stream().map(caseNoteMapper::toDto).toList());
        dto.setLabels(fraudLabelRepository.findByCaseId(id)
                .stream().map(fraudLabelMapper::toDto).toList());

        return dto;
    }

    @Transactional
    public CaseDetailDto assignCase(UUID caseId, UUID assigneeId) {
        FraudCase fraudCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + caseId));

        CaseStateMachine.validate(fraudCase.getStatus(), CaseStatus.ASSIGNED);

        fraudCase.setAssigneeId(assigneeId);
        fraudCase.setStatus(CaseStatus.ASSIGNED);
        FraudCase saved = caseRepository.save(fraudCase);

        writeAudit(saved, "CASE_ASSIGNED", "Assigned to " + assigneeId);
        eventPublisher.publishCaseAssigned(saved);
        log.info("Case {} assigned to {}", caseId, assigneeId);
        return caseMapper.toDetailDto(saved);
    }

    @Transactional
    public CaseDetailDto updateStatus(UUID caseId, CaseStatus newStatus) {
        FraudCase fraudCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + caseId));

        CaseStatus oldStatus = fraudCase.getStatus();
        doTransition(fraudCase, newStatus);
        FraudCase saved = caseRepository.save(fraudCase);

        writeAudit(saved, "STATUS_CHANGED", oldStatus + " -> " + newStatus);
        eventPublisher.publishCaseUpdated(saved);
        log.info("Case {} status: {} -> {}", caseId, oldStatus, newStatus);
        return caseMapper.toDetailDto(saved);
    }

    @Transactional(readOnly = true)
    public List<CaseNoteDto> getNotes(UUID caseId) {
        if (!caseRepository.existsById(caseId)) {
            throw new ResourceNotFoundException("Case not found with id: " + caseId);
        }
        return caseNoteRepository.findByCaseIdOrderByCreatedAtAsc(caseId)
                .stream().map(caseNoteMapper::toDto).toList();
    }

    @Transactional
    public CaseNoteDto addNote(UUID caseId, UUID authorId, CreateCaseNoteRequest request) {
        if (!caseRepository.existsById(caseId)) {
            throw new ResourceNotFoundException("Case not found with id: " + caseId);
        }
        CaseNote note = CaseNote.builder()
                .caseId(caseId)
                .authorId(authorId)
                .content(request.getContent())
                .build();
        return caseNoteMapper.toDto(caseNoteRepository.save(note));
    }

    @Transactional
    public CaseLabelDto addLabel(UUID caseId, UUID analystId, CreateCaseLabelRequest request) {
        FraudCase fraudCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + caseId));

        LabelType labelType = request.getLabel();
        CaseStatus resolvedStatus = labelType == LabelType.FRAUD
                ? CaseStatus.RESOLVED_FRAUD : CaseStatus.RESOLVED_LEGIT;

        CaseStatus oldStatus = fraudCase.getStatus();
        doTransition(fraudCase, resolvedStatus);
        FraudCase saved = caseRepository.save(fraudCase);

        FraudLabel label = FraudLabel.builder()
                .transactionId(fraudCase.getTransactionId())
                .caseId(caseId)
                .analystId(analystId)
                .label(labelType)
                .confidence(request.getConfidence())
                .reason(request.getReason())
                .build();
        FraudLabel savedLabel = fraudLabelRepository.save(label);

        writeAudit(saved, "LABEL_APPLIED",
                oldStatus + " -> " + resolvedStatus + " | label=" + labelType + " by " + analystId);
        eventPublisher.publishCaseResolved(saved);

        String topic = labelType == LabelType.FRAUD ? TOPIC_FRAUD_CONFIRMED : TOPIC_FRAUD_FALSE_POSITIVE;
        FraudLabelEvent event = new FraudLabelEvent(
                fraudCase.getTransactionId(), caseId, analystId, labelType.name(), request.getConfidence());
        publishAfterCommit(fraudCase.getTransactionId(), topic, event);

        log.info("Label {} applied to case {} by {}", labelType, caseId, analystId);
        return fraudLabelMapper.toDto(savedLabel);
    }

    private static void doTransition(FraudCase fraudCase, CaseStatus newStatus) {
        CaseStateMachine.validate(fraudCase.getStatus(), newStatus);
        fraudCase.setStatus(newStatus);
        if (newStatus == CaseStatus.RESOLVED_FRAUD || newStatus == CaseStatus.RESOLVED_LEGIT) {
            fraudCase.setResolvedAt(Instant.now());
        }
    }

    private void publishAfterCommit(UUID key, String topic, Object payload) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(key, topic, payload);
                }
            });
        } else {
            send(key, topic, payload);
        }
    }

    private void send(UUID key, String topic, Object payload) {
        @SuppressWarnings("FutureReturnValueIgnored")
        var ignored = kafkaTemplate.send(topic, key.toString(), payload)
                .whenComplete((r, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish to {}: {}", topic, ex.getMessage());
                    }
                });
    }

    private void writeAudit(FraudCase fraudCase, String action, String detail) {
        AuditEvent event = AuditEvent.builder()
                .actor("system")
                .action(action)
                .targetType("FRAUD_CASE")
                .targetId(fraudCase.getId())
                .after(detail)
                .build();
        auditEventRepository.save(event);
    }
}
