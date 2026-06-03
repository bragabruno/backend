package com.bragdev.frauddetection;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base class for container-backed integration tests. Uses the Testcontainers <em>singleton</em>
 * pattern: the containers are started once in a static initializer (guaranteed before the Spring
 * context's condition evaluation reads the datasource URL) and reused across the suite. Ryuk tears
 * them down at JVM exit, so no explicit stop is needed.
 *
 * <p>All ITs share a single Spring context (same configuration), hence a single fraud-engine Kafka
 * consumer — avoiding competing-consumer races on {@code transaction-events}. The ml-service is
 * stubbed by one in-process {@link HttpServer} whose {@code POST /predict} behaviour each test sets
 * per-method via {@link #mlServiceRespondsWith}/{@link #mlServiceDown} (default: down → 503, so the
 * {@code MlPredictionClient} circuit breaker trips and scoring runs degraded/rules-only).
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability // keep tracing on under @AutoConfigureMockMvc so FRAUD-120 propagation is exercised
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class IntegrationTest {

    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("fraud_db")
                    .withUsername("fraud_user")
                    .withPassword("fraud_password");

    static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    static final ConfluentKafkaContainer kafka =
            new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    /** Stub ml-service. {@code null} response => respond 503 (service down). */
    private static final HttpServer mlService;
    private static volatile String mlPredictResponseJson;
    private static final AtomicReference<String> lastPredictRequest = new AtomicReference<>();

    static {
        postgres.start();
        redis.start();
        kafka.start();
        try {
            mlService = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
        mlService.createContext("/predict", exchange -> {
            lastPredictRequest.set(
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String json = mlPredictResponseJson;
            if (json == null) {
                exchange.sendResponseHeaders(503, -1);
                exchange.close();
                return;
            }
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        mlService.start();
    }

    /** Make the stub ml-service answer {@code /predict} with the given JSON body. */
    protected static void mlServiceRespondsWith(String predictResponseJson) {
        mlPredictResponseJson = predictResponseJson;
    }

    /** Make the stub ml-service unavailable (503), exercising the degraded/rules-only path. */
    protected static void mlServiceDown() {
        mlPredictResponseJson = null;
    }

    /** Body of the most recent {@code /predict} request the stub received, or {@code null}. */
    protected static String lastMlPredictRequest() {
        return lastPredictRequest.get();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        // Read from the beginning so a message published before the consumer is assigned is still scored.
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("fraud.ml-service.base-url",
                () -> "http://127.0.0.1:" + mlService.getAddress().getPort());
    }
}
