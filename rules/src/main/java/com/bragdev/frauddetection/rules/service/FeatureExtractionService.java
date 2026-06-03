package com.bragdev.frauddetection.rules.service;

import com.bragdev.frauddetection.common.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Builds the real-time feature vector for a transaction from Redis-backed behavioural state.
 *
 * <p>Features feed both the deterministic {@link RuleEngine} (via {@code RuleContext}) and the
 * ML model (via {@link MlPredictionClient}). Velocity counters, new-device detection and recent
 * failed-attempt counts are all maintained in Redis for low-latency lookups.
 */
@Service
public class FeatureExtractionService {

    private static final Logger log = LoggerFactory.getLogger(FeatureExtractionService.class);

    private static final String VELOCITY_5M_PREFIX = "velocity:5m:";
    private static final String VELOCITY_24H_PREFIX = "velocity:24h:";
    private static final String SEEN_DEVICES_PREFIX = "seen-devices:";
    private static final String FAILED_ATTEMPTS_PREFIX = "failed-attempts:";

    private static final long SEEN_DEVICES_TTL_DAYS = 90;
    private static final long FAILED_ATTEMPTS_TTL_HOURS = 1;

    private final StringRedisTemplate redisTemplate;

    public FeatureExtractionService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Map<String, Object> extractFeatures(Transaction transaction) {
        Map<String, Object> features = new HashMap<>();

        String userId = transaction.getUserId().toString();
        long count5m = incrementCounter(VELOCITY_5M_PREFIX + userId, 5, TimeUnit.MINUTES);
        long count24h = incrementCounter(VELOCITY_24H_PREFIX + userId, 24, TimeUnit.HOURS);

        boolean newDevice = registerDevice(userId, transaction.getDeviceId().toString());
        int failedAttempts = currentFailedAttempts(userId);

        features.put("velocity_5m", count5m);
        features.put("velocity_24h", count24h);
        // Both keys carry the same signal: "new_device" is read by the ML request builder,
        // "device_is_new" is the key the deterministic DeviceRule looks up.
        features.put("new_device", newDevice);
        features.put("device_is_new", newDevice);
        features.put("failed_attempts", failedAttempts);
        features.put("amount", transaction.getAmount());
        features.put("currency", transaction.getCurrency());
        features.put("country", transaction.getCountry());
        features.put("device_id", transaction.getDeviceId().toString());
        features.put("merchant_id", transaction.getMerchantId().toString());

        log.debug("Extracted {} features for transaction {} (newDevice={}, failedAttempts={})",
                features.size(), transaction.getId(), newDevice, failedAttempts);
        return features;
    }

    /**
     * Records a failed/declined attempt for the user. Called by the decision pipeline when a
     * transaction is declined so that a burst of declines raises the risk of subsequent ones.
     */
    public void recordFailedAttempt(UUID userId) {
        String key = FAILED_ATTEMPTS_PREFIX + userId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, FAILED_ATTEMPTS_TTL_HOURS, TimeUnit.HOURS);
        }
    }

    private long incrementCounter(String key, long ttl, TimeUnit unit) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, ttl, unit);
        }
        return count != null ? count : 0;
    }

    /**
     * Adds the device to the user's seen-device set. Returns {@code true} when the device had not
     * been seen before (SADD reports one new member), i.e. this is a new device for the user.
     */
    private boolean registerDevice(String userId, String deviceId) {
        String key = SEEN_DEVICES_PREFIX + userId;
        Long added = redisTemplate.opsForSet().add(key, deviceId);
        boolean isNew = added != null && added == 1;
        if (isNew) {
            redisTemplate.expire(key, SEEN_DEVICES_TTL_DAYS, TimeUnit.DAYS);
        }
        return isNew;
    }

    private int currentFailedAttempts(String userId) {
        String value = redisTemplate.opsForValue().get(FAILED_ATTEMPTS_PREFIX + userId);
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
