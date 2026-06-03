package com.bragdev.frauddetection.rules.core;

import com.bragdev.frauddetection.common.model.Transaction;
import com.bragdev.frauddetection.rules.spi.Rule;
import com.bragdev.frauddetection.rules.spi.RuleContext;
import com.bragdev.frauddetection.rules.spi.RuleOutcome;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class VelocityRule implements Rule {

    private static final int VELOCITY_5M_THRESHOLD = 3;
    private static final int VELOCITY_24H_THRESHOLD = 10;

    @Override
    public String name() {
        return "velocity";
    }

    @Override
    public RuleOutcome evaluate(Transaction transaction, RuleContext context) {
        int count5m = getInt(context.features(), "velocity_5m", 0);
        int count24h = getInt(context.features(), "velocity_24h", 0);

        if (count5m > VELOCITY_5M_THRESHOLD) {
            return RuleOutcome.flagged(0.7, "VELOCITY_5M_HIGH",
                    Map.of("count_5m", count5m, "threshold", VELOCITY_5M_THRESHOLD));
        }
        if (count24h > VELOCITY_24H_THRESHOLD) {
            return RuleOutcome.flagged(0.5, "VELOCITY_24H_HIGH",
                    Map.of("count_24h", count24h, "threshold", VELOCITY_24H_THRESHOLD));
        }
        return RuleOutcome.clean();
    }

    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number n) {
            return n.intValue();
        }
        return defaultValue;
    }
}
