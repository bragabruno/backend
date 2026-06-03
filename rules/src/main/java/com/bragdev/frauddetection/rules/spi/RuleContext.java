package com.bragdev.frauddetection.rules.spi;

import java.util.Map;

public record RuleContext(
    Map<String, Object> features,
    Map<String, Object> config
) {
    public static RuleContext empty() {
        return new RuleContext(Map.of(), Map.of());
    }

    public static RuleContext withFeatures(Map<String, Object> features) {
        return new RuleContext(features, Map.of());
    }
}
