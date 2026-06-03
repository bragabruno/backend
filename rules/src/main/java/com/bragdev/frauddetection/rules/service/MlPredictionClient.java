package com.bragdev.frauddetection.rules.service;

import com.bragdev.frauddetection.common.model.Transaction;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Calls the Python ml-service {@code POST /predict} endpoint. The request and response DTOs are
 * pinned to snake_case via {@link JsonNaming} so the wire contract matches the FastAPI Pydantic
 * models regardless of the {@link RestClient}'s underlying ObjectMapper configuration.
 *
 * <p>Wrapped in a Resilience4j circuit breaker + retry; when the ml-service is unavailable the
 * fallback returns a degraded result and the {@link DecisionEngine} scores on rules alone.
 */
@Service
public class MlPredictionClient {

    private static final Logger log = LoggerFactory.getLogger(MlPredictionClient.class);
    private static final String CIRCUIT_BREAKER_NAME = "mlService";

    private final RestClient restClient;

    public MlPredictionClient(
            @Value("${fraud.ml-service.base-url:http://localhost:8000}") String baseUrl
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackPredict")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public MlPredictionResult predict(Transaction transaction, Map<String, Object> features) {
        MlPredictRequest request = buildRequest(transaction, features);
        log.debug("Calling ML service /predict for transaction {}", transaction.getId());

        MlPredictionResponse response = restClient.post()
                .uri("/predict")
                .body(request)
                .retrieve()
                .body(MlPredictionResponse.class);

        if (response == null) {
            throw new IllegalStateException("ML service returned null response");
        }

        return new MlPredictionResult(
                response.fraudProbability(),
                response.contributingFactors() != null ? response.contributingFactors() : List.of(),
                response.modelVersion(),
                false
        );
    }

    private MlPredictRequest buildRequest(Transaction transaction, Map<String, Object> features) {
        return new MlPredictRequest(
                transaction.getId() != null ? transaction.getId().toString() : null,
                transaction.getAmount() != null ? transaction.getAmount().doubleValue() : 0.0,
                booleanFeature(features, "new_device"),
                intFeature(features, "failed_attempts"),
                transaction.getCountry(),
                transaction.getCurrency(),
                idString(transaction.getMerchantId()),
                idString(transaction.getDeviceId()),
                idString(transaction.getUserId())
        );
    }

    @SuppressWarnings("unused") // invoked reflectively by Resilience4j on circuit-open / exhausted retries
    private MlPredictionResult fallbackPredict(Transaction transaction, Map<String, Object> features, Throwable ex) {
        log.warn("ML service unavailable for transaction {}, falling back to rules-only: {}",
                transaction.getId(), ex.getMessage());
        return new MlPredictionResult(0.0, List.of(), "degraded", true);
    }

    private static boolean booleanFeature(Map<String, Object> features, String key) {
        return features.get(key) instanceof Boolean b && b;
    }

    private static int intFeature(Map<String, Object> features, String key) {
        return features.get(key) instanceof Number n ? n.intValue() : 0;
    }

    private static String idString(java.util.UUID id) {
        return id != null ? id.toString() : null;
    }

    public record MlPredictionResult(
            double fraudProbability,
            List<String> contributingFactors,
            String modelVersion,
            boolean degradedMode
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record MlPredictRequest(
            String transactionId,
            double amount,
            boolean newDevice,
            int failedAttempts,
            String country,
            String currency,
            String merchantId,
            String deviceId,
            String userId
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    record MlPredictionResponse(
            String transactionId,
            double fraudProbability,
            String riskLevel,
            String modelVersion,
            List<String> contributingFactors
    ) {}
}
