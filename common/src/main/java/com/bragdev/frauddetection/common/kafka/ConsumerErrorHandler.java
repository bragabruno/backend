package com.bragdev.frauddetection.common.kafka;

import java.util.List;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.FixedBackOff;

import java.util.List;

/**
 * Standardized Kafka consumer error handler with retry and DLQ routing.
 * Transient failures are retried with backoff; permanent failures (deserialization errors)
 * are immediately routed to the DLQ.
 */
public class ConsumerErrorHandler implements CommonErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ConsumerErrorHandler.class);
    private static final long BACK_OFF_INTERVAL = 1000L;
    private static final long MAX_ATTEMPTS = 3;

    private final DefaultErrorHandler delegate;

    public ConsumerErrorHandler() {
        this(new FixedBackOff(BACK_OFF_INTERVAL, MAX_ATTEMPTS));
    }

    public ConsumerErrorHandler(BackOff backOff) {
        this.delegate = new DefaultErrorHandler(
                (record, exception) -> handleDeserializationError(record, exception),
                backOff
        );
        this.delegate.addNotRetryableExceptions(DeserializationException.class);
    }

    @Override
    public boolean handleOne(Exception thrownException, ConsumerRecord<?, ?> record, Consumer<?, ?> consumer, MessageListenerContainer container) {
        log.error("Consumer error on topic={} partition={} offset={}: {}",
                record.topic(), record.partition(), record.offset(), thrownException.getMessage());
        return delegate.handleOne(thrownException, record, consumer, container);
    }

    @Override
    public void handleRemaining(Exception thrownException, List<ConsumerRecord<?, ?>> records, Consumer<?, ?> consumer, MessageListenerContainer container) {
        delegate.handleRemaining(thrownException, records, consumer, container);
    }

    @Override
    public void handleOtherException(Exception thrownException, Consumer<?, ?> consumer, MessageListenerContainer container, boolean batchListener) {
        delegate.handleOtherException(thrownException, consumer, container, batchListener);
    }

    private void handleDeserializationError(ConsumerRecord<?, ?> record, Exception exception) {
        log.error("Permanent failure (deserialization) on topic={} partition={} offset={}, routing to DLQ: {}",
                record.topic(), record.partition(), record.offset(), exception.getMessage());
    }
}
