package com.bragdev.frauddetection.common.repository;

import com.bragdev.frauddetection.common.enums.CaseStatus;
import com.bragdev.frauddetection.common.enums.Severity;
import com.bragdev.frauddetection.common.model.FraudCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FraudCaseRepository extends JpaRepository<FraudCase, UUID> {

    Optional<FraudCase> findByTransactionId(UUID transactionId);

    Page<FraudCase> findByStatus(CaseStatus status, Pageable pageable);

    Page<FraudCase> findBySeverity(Severity severity, Pageable pageable);

    Page<FraudCase> findByAssigneeId(UUID assigneeId, Pageable pageable);

    @Query("SELECT fc FROM FraudCase fc WHERE " +
           "(:status IS NULL OR fc.status = :status) AND " +
           "(:severity IS NULL OR fc.severity = :severity) AND " +
           "(:assigneeId IS NULL OR fc.assigneeId = :assigneeId) " +
           "ORDER BY CASE fc.severity " +
           "WHEN 'CRITICAL' THEN 1 " +
           "WHEN 'HIGH' THEN 2 " +
           "WHEN 'MEDIUM' THEN 3 " +
           "WHEN 'LOW' THEN 4 END, fc.openedAt ASC")
    Page<FraudCase> findByFilters(
            @Param("status") CaseStatus status,
            @Param("severity") Severity severity,
            @Param("assigneeId") UUID assigneeId,
            Pageable pageable);
}
