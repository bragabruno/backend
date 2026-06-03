package com.bragdev.frauddetection.common.repository;

import com.bragdev.frauddetection.common.model.FraudLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FraudLabelRepository extends JpaRepository<FraudLabel, UUID> {

    List<FraudLabel> findByTransactionId(UUID transactionId);

    List<FraudLabel> findByCaseId(UUID caseId);

    List<FraudLabel> findByAnalystId(UUID analystId);
}
