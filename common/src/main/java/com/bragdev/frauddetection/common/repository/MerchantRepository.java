package com.bragdev.frauddetection.common.repository;

import com.bragdev.frauddetection.common.model.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    List<Merchant> findByMcc(String mcc);

    List<Merchant> findByCountry(String country);

    List<Merchant> findByRiskTier(com.bragdev.frauddetection.common.enums.RiskTier riskTier);
}
