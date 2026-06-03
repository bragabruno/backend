package com.bragdev.frauddetection.rules.service;

import com.bragdev.frauddetection.common.model.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureExtractionServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private SetOperations<String, String> setOps;

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID DEVICE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Test
    void flagsANewDeviceWhenSetAddReportsANewMember() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(valueOps.increment(anyString())).thenReturn(1L, 4L);
        when(setOps.add(startsWith("seen-devices:"), anyString())).thenReturn(1L); // newly added
        when(valueOps.get(startsWith("failed-attempts:"))).thenReturn("2");

        Map<String, Object> features = new FeatureExtractionService(redisTemplate).extractFeatures(transaction());

        assertThat(features.get("new_device")).isEqualTo(true);
        assertThat(features.get("device_is_new")).isEqualTo(true);
        assertThat(features.get("failed_attempts")).isEqualTo(2);
        assertThat(features.get("velocity_5m")).isEqualTo(1L);
        assertThat(features.get("velocity_24h")).isEqualTo(4L);
        // First-time seen-devices member gets a TTL applied.
        verify(redisTemplate).expire(startsWith("seen-devices:"), eq(90L), eq(TimeUnit.DAYS));
    }

    @Test
    void doesNotFlagAKnownDeviceAndDefaultsFailedAttemptsToZero() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(valueOps.increment(anyString())).thenReturn(2L);
        when(setOps.add(startsWith("seen-devices:"), anyString())).thenReturn(0L); // already present
        when(valueOps.get(startsWith("failed-attempts:"))).thenReturn(null);

        Map<String, Object> features = new FeatureExtractionService(redisTemplate).extractFeatures(transaction());

        assertThat(features.get("new_device")).isEqualTo(false);
        assertThat(features.get("device_is_new")).isEqualTo(false);
        assertThat(features.get("failed_attempts")).isEqualTo(0);
    }

    @Test
    void recordFailedAttemptIncrementsCounterAndSetsTtlOnFirstHit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment("failed-attempts:" + USER_ID)).thenReturn(1L);

        new FeatureExtractionService(redisTemplate).recordFailedAttempt(USER_ID);

        verify(redisTemplate).expire(eq("failed-attempts:" + USER_ID), anyLong(), any(TimeUnit.class));
    }

    private static Transaction transaction() {
        return Transaction.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .userId(USER_ID)
                .merchantId(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .deviceId(DEVICE_ID)
                .amount(new BigDecimal("12000.00"))
                .currency("USD")
                .country("BR")
                .build();
    }
}
