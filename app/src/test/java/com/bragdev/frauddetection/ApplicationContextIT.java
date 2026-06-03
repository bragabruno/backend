package com.bragdev.frauddetection;

import com.bragdev.frauddetection.common.enums.TransactionStatus;
import com.bragdev.frauddetection.common.model.Transaction;
import com.bragdev.frauddetection.common.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end wiring check: boots the full Spring context (transactions, rules, fraud-engine
 * Kafka consumer, case-management, security) against real Postgres, Redis and Kafka containers,
 * and round-trips a transaction through JPA. Proves the module graph actually assembles and the
 * persistence mapping is valid — not just that individual units compile.
 */
class ApplicationContextIT extends IntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void contextLoads() {
        assertThat(transactionRepository).isNotNull();
    }

    @Test
    void persistsAndReadsBackATransaction() {
        Transaction transaction = Transaction.builder()
                .userId(UUID.randomUUID())
                .merchantId(UUID.randomUUID())
                .deviceId(UUID.randomUUID())
                .amount(new BigDecimal("199.99"))
                .currency("USD")
                .country("US")
                .status(TransactionStatus.RECEIVED)
                .idempotencyKey("it-" + UUID.randomUUID())
                .build();

        Transaction saved = transactionRepository.save(transaction);

        assertThat(saved.getId()).isNotNull();
        assertThat(transactionRepository.findById(saved.getId()))
                .get()
                .satisfies(found -> {
                    assertThat(found.getAmount()).isEqualByComparingTo("199.99");
                    assertThat(found.getStatus()).isEqualTo(TransactionStatus.RECEIVED);
                    assertThat(found.getCreatedAt()).isNotNull();
                });
    }
}
