package com.bragdev.frauddetection.transactions.controller;

import com.bragdev.frauddetection.common.dto.CreateTransactionRequest;
import com.bragdev.frauddetection.common.dto.PageResponse;
import com.bragdev.frauddetection.common.dto.TransactionDto;
import com.bragdev.frauddetection.common.enums.TransactionStatus;
import com.bragdev.frauddetection.transactions.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    @GetMapping
    @Operation(summary = "List transactions (paginated), filterable by status and userId")
    public PageResponse<TransactionDto> listTransactions(
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return transactionService.listTransactions(status, userId, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
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
