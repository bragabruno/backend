package com.bragdev.frauddetection.common.repository;

import com.bragdev.frauddetection.common.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {

    Optional<Device> findByFingerprint(String fingerprint);

    boolean existsByFingerprint(String fingerprint);
}
