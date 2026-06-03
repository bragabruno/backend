package com.bragdev.frauddetection.rules.core;

import com.bragdev.frauddetection.common.model.Transaction;
import com.bragdev.frauddetection.rules.spi.Rule;
import com.bragdev.frauddetection.rules.spi.RuleContext;
import com.bragdev.frauddetection.rules.spi.RuleOutcome;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MerchantRule implements Rule {

    @Override
    public String name() {
        return "merchant";
    }

    @Override
    public RuleOutcome evaluate(Transaction transaction, RuleContext context) {
        String riskTier = getString(context.features(), "merchant_risk_tier", "LOW");
        boolean highRiskMcc = getBoolean(context.features(), "merchant_high_risk_mcc", false);

        if ("HIGH".equals(riskTier)) {
            return RuleOutcome.flagged(0.5, "HIGH_RISK_MERCHANT",
                    Map.of("merchant_id", transaction.getMerchantId().toString(), "risk_tier", riskTier));
        }
        if ("MEDIUM".equals(riskTier) && highRiskMcc) {
            return RuleOutcome.flagged(0.3, "MEDIUM_RISK_HIGH_MCC",
                    Map.of("merchant_id", transaction.getMerchantId().toString()));
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

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value instanceof String s) {
            return s;
        }
        return defaultValue;
    }
}
