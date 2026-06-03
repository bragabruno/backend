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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudScoringServiceTest {

    @Mock private DecisionEngine decisionEngine;
    @Mock private CaseService caseService;
    @Mock private TransactionRepository transactionRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    private FraudScoringService service;

    private static final UUID TXN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID RISK_SCORE_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID CASE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @BeforeEach
    void setUp() {
        service = new FraudScoringService(decisionEngine, caseService, transactionRepository, kafkaTemplate,
                new SimpleMeterRegistry());
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);
    }

    @Test
    void approveUpdatesStatusWithoutOpeningACase() {
        stubDecision(Decision.APPROVE, 0.1, List.of());
        when(transactionRepository.findById(TXN_ID)).thenReturn(Optional.of(persistedTransaction()));

        FraudDecisionEvent event = service.score(transactionEvent());

        assertThat(event.decision()).isEqualTo(Decision.APPROVE);
        assertThat(event.caseId()).isNull();
        assertThat(savedStatus()).isEqualTo(TransactionStatus.APPROVED);
        verify(caseService, never()).createCase(any(), any(), any());
        verify(kafkaTemplate).send(eq("fraud.scored"), eq(TXN_ID.toString()), any());
    }

    @Test
    void reviewOpensAMediumSeverityCase() {
        stubDecision(Decision.REVIEW, 0.45, List.of("FOREIGN_COUNTRY"));
        when(transactionRepository.findById(TXN_ID)).thenReturn(Optional.of(persistedTransaction()));
        when(caseService.createCase(eq(TXN_ID), eq(RISK_SCORE_ID), eq(Severity.MEDIUM)))
                .thenReturn(CaseDetailDto.builder().id(CASE_ID).build());

        FraudDecisionEvent event = service.score(transactionEvent());

        assertThat(event.decision()).isEqualTo(Decision.REVIEW);
        assertThat(event.caseId()).isEqualTo(CASE_ID);
        assertThat(savedStatus()).isEqualTo(TransactionStatus.IN_REVIEW);
        verify(caseService).createCase(TXN_ID, RISK_SCORE_ID, Severity.MEDIUM);
    }

    @Test
    void declineBelowCriticalThresholdOpensHighSeverityCase() {
        stubDecision(Decision.DECLINE, 0.8, List.of("VELOCITY_5M_HIGH"));
        when(transactionRepository.findById(TXN_ID)).thenReturn(Optional.of(persistedTransaction()));
        when(caseService.createCase(eq(TXN_ID), eq(RISK_SCORE_ID), eq(Severity.HIGH)))
                .thenReturn(CaseDetailDto.builder().id(CASE_ID).build());

        service.score(transactionEvent());

        assertThat(savedStatus()).isEqualTo(TransactionStatus.DECLINED);
        verify(caseService).createCase(TXN_ID, RISK_SCORE_ID, Severity.HIGH);
    }

    @Test
    void declineAtOrAboveCriticalThresholdOpensCriticalCase() {
        stubDecision(Decision.DECLINE, 0.95, List.of("IMPOSSIBLE_TRAVEL"));
        when(transactionRepository.findById(TXN_ID)).thenReturn(Optional.of(persistedTransaction()));
        when(caseService.createCase(eq(TXN_ID), eq(RISK_SCORE_ID), eq(Severity.CRITICAL)))
                .thenReturn(CaseDetailDto.builder().id(CASE_ID).build());

        service.score(transactionEvent());

        verify(caseService).createCase(TXN_ID, RISK_SCORE_ID, Severity.CRITICAL);
    }

    @Test
    void stillScoresAndPublishesWhenTransactionRowIsMissing() {
        stubDecision(Decision.APPROVE, 0.1, List.of());
        when(transactionRepository.findById(TXN_ID)).thenReturn(Optional.empty());

        FraudDecisionEvent event = service.score(transactionEvent());

        assertThat(event.decision()).isEqualTo(Decision.APPROVE);
        verify(transactionRepository, never()).save(any());
        verify(kafkaTemplate).send(eq("fraud.scored"), eq(TXN_ID.toString()), any());
    }

    private void stubDecision(Decision decision, double score, List<String> reasonCodes) {
        when(decisionEngine.evaluate(any()))
                .thenReturn(new DecisionEngine.DecisionResult(decision, score, reasonCodes, false, RISK_SCORE_ID));
    }

    private TransactionStatus savedStatus() {
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        return captor.getValue().getStatus();
    }

    private static TransactionEvent transactionEvent() {
        return new TransactionEvent(
                TXN_ID,
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                new BigDecimal("12000.00"),
                "USD",
                "10.0.0.1",
                "BR",
                Instant.parse("2026-06-03T00:00:00Z"));
    }

    private static Transaction persistedTransaction() {
        return Transaction.builder()
                .id(TXN_ID)
                .userId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .merchantId(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .deviceId(UUID.fromString("44444444-4444-4444-4444-444444444444"))
                .amount(new BigDecimal("12000.00"))
                .currency("USD")
                .country("BR")
                .status(TransactionStatus.RECEIVED)
                .build();
    }
}
