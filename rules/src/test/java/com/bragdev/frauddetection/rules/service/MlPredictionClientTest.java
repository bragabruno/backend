package com.bragdev.frauddetection.rules.service;

import com.bragdev.frauddetection.common.model.Transaction;
import com.bragdev.frauddetection.rules.service.MlPredictionClient.MlPredictionResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract test that pins the backend &lt;-&gt; ml-service HTTP seam: the request must be sent in the
 * snake_case shape FastAPI's {@code PredictRequest} expects, and the snake_case {@code PredictResponse}
 * must be deserialized correctly (including {@code contributing_factors} as a string list and an
 * unknown {@code agent_triage} field being ignored).
 */
class MlPredictionClientTest {

    private MockWebServer server;
    private MlPredictionClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        client = new MlPredictionClient(server.url("/").toString());
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void sendsSnakeCaseRequestAndMapsSnakeCaseResponse() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "transaction_id": "11111111-1111-1111-1111-111111111111",
                          "fraud_probability": 0.83,
                          "risk_level": "HIGH",
                          "model_version": "xgb-2026-01",
                          "contributing_factors": ["HIGH_AMOUNT", "NEW_DEVICE"],
                          "agent_triage": null
                        }
                        """));

        UUID txnId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Transaction txn = Transaction.builder()
                .id(txnId)
                .userId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .merchantId(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .deviceId(UUID.fromString("44444444-4444-4444-4444-444444444444"))
                .amount(new BigDecimal("12000.00"))
                .currency("USD")
                .country("BR")
                .build();
        Map<String, Object> features = new HashMap<>();
        features.put("new_device", true);
        features.put("failed_attempts", 3L);

        MlPredictionResult result = client.predict(txn, features);

        assertThat(result.fraudProbability()).isEqualTo(0.83);
        assertThat(result.modelVersion()).isEqualTo("xgb-2026-01");
        assertThat(result.contributingFactors()).containsExactly("HIGH_AMOUNT", "NEW_DEVICE");
        assertThat(result.degradedMode()).isFalse();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo("/predict");
        JsonNode body = mapper.readTree(request.getBody().readUtf8());
        assertThat(body.get("transaction_id").asText()).isEqualTo(txnId.toString());
        assertThat(body.get("new_device").asBoolean()).isTrue();
        assertThat(body.get("failed_attempts").asInt()).isEqualTo(3);
        assertThat(body.get("amount").asDouble()).isEqualTo(12000.0);
        assertThat(body.get("country").asText()).isEqualTo("BR");
        assertThat(body.get("currency").asText()).isEqualTo("USD");
        assertThat(body.get("user_id").asText()).isEqualTo("22222222-2222-2222-2222-222222222222");
    }

    @Test
    void defaultsMissingFeaturesToSafeValues() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "transaction_id": "11111111-1111-1111-1111-111111111111",
                          "fraud_probability": 0.10,
                          "risk_level": "LOW",
                          "model_version": "xgb-2026-01",
                          "contributing_factors": []
                        }
                        """));

        Transaction txn = Transaction.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .userId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .merchantId(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .deviceId(UUID.fromString("44444444-4444-4444-4444-444444444444"))
                .amount(new BigDecimal("25.00"))
                .currency("USD")
                .country("US")
                .build();

        MlPredictionResult result = client.predict(txn, Map.of());

        assertThat(result.contributingFactors()).isEmpty();
        assertThat(result.degradedMode()).isFalse();

        JsonNode body = mapper.readTree(server.takeRequest().getBody().readUtf8());
        assertThat(body.get("new_device").asBoolean()).isFalse();
        assertThat(body.get("failed_attempts").asInt()).isZero();
    }
}
