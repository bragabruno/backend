package com.bragdev.frauddetection.rules.service;

import com.bragdev.frauddetection.common.model.Transaction;
import com.bragdev.frauddetection.rules.service.RuleEngine.RuleEvaluationResult;
import com.bragdev.frauddetection.rules.spi.Rule;
import com.bragdev.frauddetection.rules.spi.RuleContext;
import com.bragdev.frauddetection.rules.spi.RuleOutcome;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineTest {

    @Test
    void aggregatesUsingMaxScoreAndCollectsReasonCodes() {
        RuleEngine engine = new RuleEngine(List.of(
                fixedRule("a", RuleOutcome.flagged(0.3, "A", Map.of())),
                fixedRule("b", RuleOutcome.flagged(0.7, "B", Map.of())),
                fixedRule("c", RuleOutcome.clean())));

        RuleEvaluationResult result = engine.evaluate(transaction(), Map.of());

        assertThat(result.score()).isEqualTo(0.7);
        assertThat(result.reasonCodes()).containsExactly("A", "B");
        assertThat(result.outcomes()).hasSize(2);
    }

    @Test
    void isolatesAFailingRuleAsCleanInsteadOfFailingTheBatch() {
        Rule throwing = new Rule() {
            @Override
            public String name() {
                return "boom";
            }

            @Override
            public RuleOutcome evaluate(Transaction transaction, RuleContext context) {
                throw new IllegalStateException("rule blew up");
            }
        };

        RuleEngine engine = new RuleEngine(List.of(
                fixedRule("a", RuleOutcome.flagged(0.4, "A", Map.of())),
                throwing));

        RuleEvaluationResult result = engine.evaluate(transaction(), Map.of());

        assertThat(result.score()).isEqualTo(0.4);
        assertThat(result.reasonCodes()).containsExactly("A");
    }

    @Test
    void returnsZeroScoreWhenNoRuleFlags() {
        RuleEngine engine = new RuleEngine(List.of(
                fixedRule("a", RuleOutcome.clean()),
                fixedRule("b", RuleOutcome.clean())));

        RuleEvaluationResult result = engine.evaluate(transaction(), Map.of());

        assertThat(result.score()).isZero();
        assertThat(result.reasonCodes()).isEmpty();
    }

    private static Rule fixedRule(String name, RuleOutcome outcome) {
        return new Rule() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public RuleOutcome evaluate(Transaction transaction, RuleContext context) {
                return outcome;
            }
        };
    }

    private static Transaction transaction() {
        return Transaction.builder()
                .id(java.util.UUID.randomUUID())
                .build();
    }
}
