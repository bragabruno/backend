package com.bragdev.frauddetection.rules.service;

import com.bragdev.frauddetection.common.enums.Decision;
import com.bragdev.frauddetection.common.model.RiskScore;
import com.bragdev.frauddetection.common.model.Transaction;
import com.bragdev.frauddetection.common.repository.RiskScoreRepository;
import com.bragdev.frauddetection.rules.service.MlPredictionClient.MlPredictionResult;
import com.bragdev.frauddetection.rules.service.RuleEngine.RuleEvaluationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DecisionEngineTest {

    @Mock private RuleEngine ruleEngine;
    @Mock private FeatureExtractionService featureExtractionService;
    @Mock private MlPredictionClient mlPredictionClient;
    @Mock private RiskScoreRepository riskScoreRepository;

    private DecisionEngine decisionEngine;

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID RISK_SCORE_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @BeforeEach
    void setUp() {
        decisionEngine = new DecisionEngine(ruleEngine, featureExtractionService, mlPredictionClient, riskScoreRepository);
        ReflectionTestUtils.setField(decisionEngine, "mlWeight", 0.6);
        ReflectionTestUtils.setField(decisionEngine, "rulesWeight", 0.4);
        ReflectionTestUtils.setField(decisionEngine, "approveThreshold", 0.3);
        ReflectionTestUtils.setField(decisionEngine, "declineThreshold", 0.7);

        when(featureExtractionService.extractFeatures(any())).thenReturn(Map.of());
        when(riskScoreRepository.save(any())).thenAnswer(invocation -> {
            RiskScore score = invocation.getArgument(0);
            score.setId(RISK_SCORE_ID);
            return score;
        });
    }

    @Test
    void declinesWhenFusedScoreExceedsDeclineThreshold() {
        stubRules(0.5, List.of("VELOCITY_5M_HIGH"));
        stubMl(0.9, List.of("HIGH_AMOUNT"));

        DecisionEngine.DecisionResult result = decisionEngine.evaluate(transaction());

        // 0.6 * 0.9 + 0.4 * 0.5 = 0.74 -> DECLINE
        assertThat(result.score()).isEqualTo(0.74);
        assertThat(result.decision()).isEqualTo(Decision.DECLINE);
        assertThat(result.riskScoreId()).isEqualTo(RISK_SCORE_ID);
        assertThat(result.degradedMode()).isFalse();
        verify(featureExtractionService, times(1)).recordFailedAttempt(USER_ID);
    }

    @Test
    void reviewsInTheMiddleBandAndDoesNotRecordFailedAttempt() {
        stubRules(0.4, List.of("FOREIGN_COUNTRY"));
        stubMl(0.5, List.of());

        DecisionEngine.DecisionResult result = decisionEngine.evaluate(transaction());

        // 0.6 * 0.5 + 0.4 * 0.4 = 0.46 -> REVIEW
        assertThat(result.decision()).isEqualTo(Decision.REVIEW);
        verify(featureExtractionService, never()).recordFailedAttempt(any());
    }

    @Test
    void approvesWhenFusedScoreIsLow() {
        stubRules(0.0, List.of());
        stubMl(0.1, List.of());

        DecisionEngine.DecisionResult result = decisionEngine.evaluate(transaction());

        assertThat(result.decision()).isEqualTo(Decision.APPROVE);
        verify(featureExtractionService, never()).recordFailedAttempt(any());
    }

    @Test
    void fallsBackToRulesOnlyScoreInDegradedMode() {
        stubRules(0.8, List.of("VELOCITY_5M_HIGH"));
        when(mlPredictionClient.predict(any(), any()))
                .thenReturn(new MlPredictionResult(0.0, List.of(), "degraded", true));

        DecisionEngine.DecisionResult result = decisionEngine.evaluate(transaction());

        // Degraded: aggregate == rules score (0.8) -> DECLINE, no ML factors merged
        assertThat(result.score()).isEqualTo(0.8);
        assertThat(result.decision()).isEqualTo(Decision.DECLINE);
        assertThat(result.degradedMode()).isTrue();
        assertThat(result.reasonCodes()).containsExactly("VELOCITY_5M_HIGH");
    }

    @Test
    void mergesMlContributingFactorsIntoReasonCodes() {
        stubRules(0.5, List.of("VELOCITY_5M_HIGH"));
        stubMl(0.9, List.of("HIGH_AMOUNT", "NEW_DEVICE"));

        DecisionEngine.DecisionResult result = decisionEngine.evaluate(transaction());

        assertThat(result.reasonCodes())
                .containsExactly("VELOCITY_5M_HIGH", "ML_HIGH_AMOUNT", "ML_NEW_DEVICE");
    }

    private void stubRules(double score, List<String> reasonCodes) {
        when(ruleEngine.evaluate(any(), any()))
                .thenReturn(new RuleEvaluationResult(score, reasonCodes, List.of()));
    }

    private void stubMl(double probability, List<String> factors) {
        when(mlPredictionClient.predict(any(), any()))
                .thenReturn(new MlPredictionResult(probability, factors, "xgb-2026-01", false));
    }

    private static Transaction transaction() {
        return Transaction.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .userId(USER_ID)
                .merchantId(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .deviceId(UUID.fromString("44444444-4444-4444-4444-444444444444"))
                .amount(new BigDecimal("12000.00"))
                .currency("USD")
                .country("BR")
                .build();
    }
}
