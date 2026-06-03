package com.bragdev.frauddetection.rules.service;

import com.bragdev.frauddetection.common.model.Transaction;
import com.bragdev.frauddetection.rules.spi.Rule;
import com.bragdev.frauddetection.rules.spi.RuleContext;
import com.bragdev.frauddetection.rules.spi.RuleOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RuleEngine {

    private static final Logger log = LoggerFactory.getLogger(RuleEngine.class);

    private final List<Rule> rules;

    public RuleEngine(List<Rule> rules) {
        this.rules = rules;
        log.info("RuleEngine initialized with {} rules: {}", rules.size(),
                rules.stream().map(Rule::name).collect(Collectors.joining(", ")));
    }

    public RuleEvaluationResult evaluate(Transaction transaction, Map<String, Object> features) {
        RuleContext context = RuleContext.withFeatures(features);
        List<RuleOutcome> outcomes = rules.stream()
                .map(rule -> {
                    try {
                        return rule.evaluate(transaction, context);
                    } catch (Exception e) {
                        log.error("Rule {} failed: {}", rule.name(), e.getMessage());
                        return RuleOutcome.clean();
                    }
                })
                .filter(outcome -> outcome.score() > 0)
                .collect(Collectors.toList());

        double maxScore = outcomes.stream()
                .mapToDouble(RuleOutcome::score)
                .max()
                .orElse(0.0);

        List<String> reasonCodes = outcomes.stream()
                .map(RuleOutcome::reasonCode)
                .collect(Collectors.toList());

        return new RuleEvaluationResult(maxScore, reasonCodes, outcomes);
    }

    public record RuleEvaluationResult(
            double score,
            List<String> reasonCodes,
            List<RuleOutcome> outcomes
    ) {}
}
