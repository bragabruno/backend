package com.bragdev.frauddetection.rules.core;

import com.bragdev.frauddetection.common.model.Transaction;
import com.bragdev.frauddetection.rules.spi.Rule;
import com.bragdev.frauddetection.rules.spi.RuleContext;
import com.bragdev.frauddetection.rules.spi.RuleOutcome;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GeoRule implements Rule {

    @Override
    public String name() {
        return "geo";
    }

    @Override
    public RuleOutcome evaluate(Transaction transaction, RuleContext context) {
        boolean impossibleTravel = getBoolean(context.features(), "geo_impossible_travel", false);
        boolean foreignCountry = getBoolean(context.features(), "geo_foreign_country", false);

        if (impossibleTravel) {
            return RuleOutcome.flagged(0.8, "IMPOSSIBLE_TRAVEL",
                    Map.of("country", transaction.getCountry() != null ? transaction.getCountry() : "unknown"));
        }
        if (foreignCountry) {
            return RuleOutcome.flagged(0.4, "FOREIGN_COUNTRY",
                    Map.of("country", transaction.getCountry() != null ? transaction.getCountry() : "unknown"));
        }
        return RuleOutcome.clean();
    }

    private boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean b) {
            return b;
        }
        return defaultValue;
    }
}
