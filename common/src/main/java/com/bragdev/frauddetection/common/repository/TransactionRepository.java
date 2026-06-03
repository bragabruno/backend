package com.bragdev.frauddetection.common.repository;

import com.bragdev.frauddetection.common.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    Page<Transaction> findByUserId(UUID userId, Pageable pageable);

    Page<Transaction> findByMerchantId(UUID merchantId, Pageable pageable);

    Page<Transaction> findByDeviceId(UUID deviceId, Pageable pageable);

    Page<Transaction> findByStatus(com.bragdev.frauddetection.common.enums.TransactionStatus status, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:userId IS NULL OR t.userId = :userId) AND " +
           "(:merchantId IS NULL OR t.merchantId = :merchantId)")
    Page<Transaction> findByFilters(
            @Param("status") com.bragdev.frauddetection.common.enums.TransactionStatus status,
            @Param("userId") UUID userId,
            @Param("merchantId") UUID merchantId,
            Pageable pageable);
}
