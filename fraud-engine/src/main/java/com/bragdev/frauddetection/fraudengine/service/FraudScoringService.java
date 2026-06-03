package com.bragdev.frauddetection.fraudengine.service;

import com.bragdev.frauddetection.casemanagement.service.CaseService;
import com.bragdev.frauddetection.common.dto.CaseDetailDto;
import com.bragdev.frauddetection.common.enums.Decision;
import com.bragdev.frauddetection.common.enums.Severity;
import com.bragdev.frauddetection.common.enums.TransactionStatus;
import com.bragdev.frauddetection.common.event.FraudDecisionEvent;
import com.bragdev.frauddetection.common.event.TransactionEvent;
import com.bragdev.frauddetection.common.model.Transaction;
import com.bragdev.frauddetection.common.repository.TransactionRepository;
import com.bragdev.frauddetection.rules.service.DecisionEngine;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Orchestrates fraud scoring for an incoming {@link TransactionEvent}: runs the
 * {@link DecisionEngine}, persists the resulting transaction status, opens a fraud case for
 * anything that needs analyst attention, and emits a {@link FraudDecisionEvent} for downstream
 * consumers (the analyst console SSE feed, analytics).
 */
@Service
public class FraudScoringService {

    private static final Logger log = LoggerFactory.getLogger(FraudScoringService.class);
    private static final String DECISIONS_TOPIC = "fraud.scored";
    private static final double CRITICAL_SCORE_THRESHOLD = 0.9;

    private final DecisionEngine decisionEngine;
    private final CaseService caseService;
    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Timer scoringTimer;
    private final Counter approveCounter;
    private final Counter reviewCounter;
    private final Counter declineCounter;

    public FraudScoringService(
            DecisionEngine decisionEngine,
            CaseService caseService,
            TransactionRepository transactionRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            MeterRegistry meterRegistry
    ) {
        this.decisionEngine = decisionEngine;
        this.caseService = caseService;
        this.transactionRepository = transactionRepository;
        this.kafkaTemplate = kafkaTemplate;

        this.scoringTimer = Timer.builder("fraud.scoring.duration")
                .description("Time spent evaluating fraud scoring pipeline")
                .register(meterRegistry);
        this.approveCounter = decisionCounter(meterRegistry, "APPROVE");
        this.reviewCounter = decisionCounter(meterRegistry, "REVIEW");
        this.declineCounter = decisionCounter(meterRegistry, "DECLINE");
    }

    private static Counter decisionCounter(MeterRegistry registry, String decision) {
        return Counter.builder("fraud.decisions")
                .tag("decision", decision)
                .description("Total fraud decisions by outcome")
                .register(registry);
    }

    @Transactional
    public FraudDecisionEvent score(TransactionEvent event) {
        Transaction transaction = event.toTransaction();
        DecisionEngine.DecisionResult result = scoringTimer.record(() -> decisionEngine.evaluate(transaction));

        counterFor(result.decision()).increment();

        updateTransactionStatus(event.transactionId(), result.decision());

        UUID caseId = null;
        if (requiresCase(result.decision())) {
            CaseDetailDto fraudCase = caseService.createCase(
                    event.transactionId(), result.riskScoreId(), severityFor(result));
            caseId = fraudCase.getId();
        }

        FraudDecisionEvent decisionEvent = new FraudDecisionEvent(
                event.transactionId(),
                result.riskScoreId(),
                caseId,
                result.decision(),
                result.score(),
                result.degradedMode(),
                result.reasonCodes());

        publish(event.transactionId(), decisionEvent);
        return decisionEvent;
    }

    private Counter counterFor(Decision decision) {
        return switch (decision) {
            case APPROVE -> approveCounter;
            case REVIEW -> reviewCounter;
            case DECLINE -> declineCounter;
        };
    }

    private static boolean requiresCase(Decision decision) {
        return decision == Decision.REVIEW || decision == Decision.DECLINE;
    }

    private void updateTransactionStatus(UUID transactionId, Decision decision) {
        transactionRepository.findById(transactionId).ifPresentOrElse(
                txn -> {
                    txn.setStatus(statusFor(decision));
                    transactionRepository.save(txn);
                },
                () -> log.warn("Transaction {} not found while applying decision {}", transactionId, decision));
    }

    private static TransactionStatus statusFor(Decision decision) {
        return switch (decision) {
            case APPROVE -> TransactionStatus.APPROVED;
            case REVIEW -> TransactionStatus.IN_REVIEW;
            case DECLINE -> TransactionStatus.DECLINED;
        };
    }

    private static Severity severityFor(DecisionEngine.DecisionResult result) {
        if (result.decision() == Decision.DECLINE) {
            return result.score() >= CRITICAL_SCORE_THRESHOLD ? Severity.CRITICAL : Severity.HIGH;
        }
        return Severity.MEDIUM;
    }

    private void publish(UUID transactionId, FraudDecisionEvent event) {
        @SuppressWarnings("FutureReturnValueIgnored")
        var ignored = kafkaTemplate.send(DECISIONS_TOPIC, transactionId.toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish fraud decision for transaction {}", transactionId, ex);
                    } else {
                        log.debug("Fraud decision published for transaction {}", transactionId);
                    }
                });
    }
}
