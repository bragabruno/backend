package com.bragdev.frauddetection.rules.service;

import com.bragdev.frauddetection.common.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class FeatureExtractionService {

    private static final Logger log = LoggerFactory.getLogger(FeatureExtractionService.class);
    private static final String VELOCITY_5M_PREFIX = "velocity:5m:";
    private static final String VELOCITY_24H_PREFIX = "velocity:24h:";

    private final StringRedisTemplate redisTemplate;

    public FeatureExtractionService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Map<String, Object> extractFeatures(Transaction transaction) {
        Map<String, Object> features = new HashMap<>();

        String userId = transaction.getUserId().toString();
        long count5m = incrementCounter(VELOCITY_5M_PREFIX + userId, 5, TimeUnit.MINUTES);
        long count24h = incrementCounter(VELOCITY_24H_PREFIX + userId, 24, TimeUnit.HOURS);

        features.put("velocity_5m", count5m);
        features.put("velocity_24h", count24h);
        features.put("amount", transaction.getAmount());
        features.put("currency", transaction.getCurrency());
        features.put("country", transaction.getCountry());
        features.put("device_id", transaction.getDeviceId().toString());
        features.put("merchant_id", transaction.getMerchantId().toString());

        log.debug("Extracted {} features for transaction {}", features.size(), transaction.getId());
        return features;
    }

    private long incrementCounter(String key, long ttl, TimeUnit unit) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, ttl, unit);
        }
        return count != null ? count : 0;
    }
}
