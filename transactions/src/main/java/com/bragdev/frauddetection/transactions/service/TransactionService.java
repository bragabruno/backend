package com.bragdev.frauddetection.transactions.service;

import com.bragdev.frauddetection.common.dto.CreateTransactionRequest;
import com.bragdev.frauddetection.common.dto.TransactionDto;
import com.bragdev.frauddetection.common.enums.TransactionStatus;
import com.bragdev.frauddetection.common.mapper.TransactionMapper;
import com.bragdev.frauddetection.common.model.Transaction;
import com.bragdev.frauddetection.common.repository.TransactionRepository;
import com.bragdev.frauddetection.common.config.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private static final String TRANSACTION_EVENTS_TOPIC = "transaction-events";

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TransactionService(
            TransactionRepository transactionRepository,
            TransactionMapper transactionMapper,
            KafkaTemplate<String, Object> kafkaTemplate
    ) {
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public TransactionDto createTransaction(CreateTransactionRequest request) {
        if (transactionRepository.findByIdempotencyKey(request.getIdempotencyKey()).isPresent()) {
            throw new IllegalArgumentException("Duplicate idempotency key");
        }

        Transaction transaction = Transaction.builder()
                .userId(request.getUserId())
                .merchantId(request.getMerchantId())
                .deviceId(request.getDeviceId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .ipAddress(request.getIpAddress())
                .country(request.getCountry())
                .status(TransactionStatus.RECEIVED)
                .idempotencyKey(request.getIdempotencyKey())
                .build();

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transaction created: {}", saved.getIdempotencyKey());

        @SuppressWarnings("FutureReturnValueIgnored")
        var ignored = kafkaTemplate.send(TRANSACTION_EVENTS_TOPIC, saved.getId().toString(), saved)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish transaction event: {}", saved.getId(), ex);
                    } else {
                        log.debug("Transaction event published to Kafka: {}", saved.getId());
                    }
                });

        return transactionMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public TransactionDto getTransaction(UUID id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
        return transactionMapper.toDto(transaction);
    }

    @Transactional(readOnly = true)
    public TransactionDto getTransactionByIdempotencyKey(String idempotencyKey) {
        Transaction transaction = transactionRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with idempotency key: " + idempotencyKey));
        return transactionMapper.toDto(transaction);
    }
}
