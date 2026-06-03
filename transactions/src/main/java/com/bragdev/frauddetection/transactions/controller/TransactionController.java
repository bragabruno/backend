package com.bragdev.frauddetection.transactions.controller;

import com.bragdev.frauddetection.common.dto.CreateTransactionRequest;
import com.bragdev.frauddetection.common.dto.TransactionDto;
import com.bragdev.frauddetection.transactions.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Transaction ingestion and query endpoints")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @Operation(summary = "Create a new transaction")
    public TransactionDto createTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        return transactionService.createTransaction(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by ID")
    public TransactionDto getTransaction(@PathVariable UUID id) {
        return transactionService.getTransaction(id);
    }

    @GetMapping("/idempotency/{idempotencyKey}")
    @Operation(summary = "Get transaction by idempotency key")
    public TransactionDto getTransactionByIdempotencyKey(@PathVariable String idempotencyKey) {
        return transactionService.getTransactionByIdempotencyKey(idempotencyKey);
    }
}
