package com.bragdev.frauddetection.config;

import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.TextMapPropagator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Explicitly registers the W3C trace-context propagator (FRAUD-120).
 *
 * <p>With this Spring Boot / micrometer-tracing-bridge-otel / OpenTelemetry version combination the
 * auto-configured propagator resolved to {@code NoopTextMapPropagator}, so no {@code traceparent}
 * header was ever injected and traces did not link across the HTTP and Kafka boundaries. Boot builds
 * its {@code ContextPropagators} from the {@link TextMapPropagator} beans in the context, so
 * contributing the W3C propagator here makes propagation work end to end.
 */
@Configuration
public class TracingConfig {

    @Bean
    TextMapPropagator w3cTraceContextPropagator() {
        return W3CTraceContextPropagator.getInstance();
    }
}
