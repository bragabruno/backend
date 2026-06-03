package com.bragdev.frauddetection.rules.service;

import com.bragdev.frauddetection.common.enums.Decision;
import com.bragdev.frauddetection.common.model.RiskScore;
import com.bragdev.frauddetection.common.model.Transaction;
import com.bragdev.frauddetection.common.repository.RiskScoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class DecisionEngine {

    private static final Logger log = LoggerFactory.getLogger(DecisionEngine.class);

    private final RuleEngine ruleEngine;
    private final FeatureExtractionService featureExtractionService;
    private final MlPredictionClient mlPredictionClient;
    private final RiskScoreRepository riskScoreRepository;

    @Value("${fraud.decision.weight.ml:0.6}")
    private double mlWeight;

    @Value("${fraud.decision.weight.rules:0.4}")
    private double rulesWeight;

    @Value("${fraud.decision.threshold.approve:0.3}")
    private double approveThreshold;

    @Value("${fraud.decision.threshold.decline:0.7}")
    private double declineThreshold;

    public DecisionEngine(
            RuleEngine ruleEngine,
            FeatureExtractionService featureExtractionService,
            MlPredictionClient mlPredictionClient,
            RiskScoreRepository riskScoreRepository
    ) {
        this.ruleEngine = ruleEngine;
        this.featureExtractionService = featureExtractionService;
        this.mlPredictionClient = mlPredictionClient;
        this.riskScoreRepository = riskScoreRepository;
    }

    @Transactional
    public DecisionResult evaluate(Transaction transaction) {
        Map<String, Object> features = featureExtractionService.extractFeatures(transaction);

        RuleEngine.RuleEvaluationResult rulesResult = ruleEngine.evaluate(transaction, features);

        MlPredictionClient.MlPredictionResult mlResult = mlPredictionClient.predict(features);

        double aggregateScore;
        if (mlResult.degradedMode()) {
            aggregateScore = rulesResult.score();
            log.info("Degraded mode: using rules-only score {}", aggregateScore);
        } else {
            aggregateScore = (mlWeight * mlResult.fraudProbability()) + (rulesWeight * rulesResult.score());
        }

        Decision decision = determineDecision(aggregateScore);

        List<String> reasonCodes = rulesResult.reasonCodes();

        RiskScore riskScore = RiskScore.builder()
                .transactionId(transaction.getId())
                .mlScore(mlResult.degradedMode() ? 0.0 : mlResult.fraudProbability())
                .rulesScore(rulesResult.score())
                .aggregateScore(aggregateScore)
                .decision(decision)
                .reasonCodes(reasonCodes)
                .degradedMode(mlResult.degradedMode())
                .build();

        riskScoreRepository.save(riskScore);

        log.info("Decision {} for transaction {} (score: {})", decision, transaction.getId(), aggregateScore);
        return new DecisionResult(decision, aggregateScore, rulesResult.reasonCodes(), mlResult.degradedMode());
    }

    private Decision determineDecision(double score) {
        if (score >= declineThreshold) {
            return Decision.DECLINE;
        }
        if (score >= approveThreshold) {
            return Decision.REVIEW;
        }
        return Decision.APPROVE;
    }

    public record DecisionResult(
            Decision decision,
            double score,
            java.util.List<String> reasonCodes,
            boolean degradedMode
    ) {}
}
