package com.bragdev.frauddetection.rules.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

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
    public MlPredictionResult predict(Map<String, Object> features) {
        log.debug("Calling ML service for prediction");
        var response = restClient.post()
                .uri("/predict")
                .body(features)
                .retrieve()
                .body(MlPredictionResponse.class);

        if (response == null) {
            throw new RuntimeException("ML service returned null response");
        }

        return new MlPredictionResult(
                response.fraudProbability(),
                response.factors(),
                response.modelVersion(),
                false
        );
    }

    @SuppressWarnings("unused")
    private MlPredictionResult fallbackPredict(Map<String, Object> features, Throwable ex) {
        log.warn("ML service unavailable, falling back to rules-only: {}", ex.getMessage());
        return new MlPredictionResult(0.0, Map.of(), "degraded", true);
    }

    public record MlPredictionResult(
            double fraudProbability,
            Map<String, Double> factors,
            String modelVersion,
            boolean degradedMode
    ) {}

    private record MlPredictionResponse(
            double fraudProbability,
            Map<String, Double> factors,
            String modelVersion
    ) {}
}
