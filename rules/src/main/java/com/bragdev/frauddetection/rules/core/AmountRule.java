package com.bragdev.frauddetection.rules.core;

import com.bragdev.frauddetection.common.model.Transaction;
import com.bragdev.frauddetection.rules.spi.Rule;
import com.bragdev.frauddetection.rules.spi.RuleContext;
import com.bragdev.frauddetection.rules.spi.RuleOutcome;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class AmountRule implements Rule {

    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("10000.00");
    private static final BigDecimal VERY_HIGH_AMOUNT_THRESHOLD = new BigDecimal("50000.00");

    @Override
    public String name() {
        return "amount";
    }

    @Override
    public RuleOutcome evaluate(Transaction transaction, RuleContext context) {
        BigDecimal amount = transaction.getAmount();
        BigDecimal baseline = getBigDecimal(context.features(), "amount_baseline", BigDecimal.ZERO);

        if (amount.compareTo(VERY_HIGH_AMOUNT_THRESHOLD) > 0) {
            return RuleOutcome.flagged(0.6, "VERY_HIGH_AMOUNT",
                    Map.of("amount", amount, "threshold", VERY_HIGH_AMOUNT_THRESHOLD));
        }
        if (amount.compareTo(HIGH_AMOUNT_THRESHOLD) > 0) {
            return RuleOutcome.flagged(0.3, "HIGH_AMOUNT",
                    Map.of("amount", amount, "threshold", HIGH_AMOUNT_THRESHOLD));
        }
        if (baseline.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(baseline.multiply(new BigDecimal("3"))) > 0) {
            return RuleOutcome.flagged(0.4, "AMOUNT_EXCEEDS_BASELINE",
                    Map.of("amount", amount, "baseline", baseline));
        }
        return RuleOutcome.clean();
    }

    private BigDecimal getBigDecimal(Map<String, Object> map, String key, BigDecimal defaultValue) {
        Object value = map.get(key);
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        return defaultValue;
    }
}
