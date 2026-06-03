package com.bragdev.frauddetection.rules.core;

import com.bragdev.frauddetection.common.model.Transaction;
import com.bragdev.frauddetection.rules.spi.Rule;
import com.bragdev.frauddetection.rules.spi.RuleContext;
import com.bragdev.frauddetection.rules.spi.RuleOutcome;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DeviceRule implements Rule {

    @Override
    public String name() {
        return "device";
    }

    @Override
    public RuleOutcome evaluate(Transaction transaction, RuleContext context) {
        boolean isNewDevice = getBoolean(context.features(), "device_is_new", false);
        boolean isUntrusted = getBoolean(context.features(), "device_is_untrusted", false);

        if (isNewDevice) {
            return RuleOutcome.flagged(0.3, "NEW_DEVICE",
                    Map.of("device_id", transaction.getDeviceId().toString()));
        }
        if (isUntrusted) {
            return RuleOutcome.flagged(0.4, "UNTRUSTED_DEVICE",
                    Map.of("device_id", transaction.getDeviceId().toString()));
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
