package com.bragdev.frauddetection.common.repository;

import com.bragdev.frauddetection.common.model.RiskScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RiskScoreRepository extends JpaRepository<RiskScore, UUID> {

    Optional<RiskScore> findByTransactionId(UUID transactionId);

    @Query("SELECT rs FROM RiskScore rs WHERE rs.transactionId = :transactionId ORDER BY rs.createdAt DESC LIMIT 1")
    Optional<RiskScore> findLatestByTransactionId(@Param("transactionId") UUID transactionId);
}
