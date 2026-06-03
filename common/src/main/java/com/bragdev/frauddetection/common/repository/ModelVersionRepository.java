package com.bragdev.frauddetection.common.repository;

import com.bragdev.frauddetection.common.enums.ModelStatus;
import com.bragdev.frauddetection.common.model.ModelVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModelVersionRepository extends JpaRepository<ModelVersion, UUID> {

    Optional<ModelVersion> findByVersion(String version);

    Optional<ModelVersion> findByStatus(ModelStatus status);

    /**
     * The last-known-good model version: the most recently deployed version in the given status
     * (typically {@link ModelStatus#ARCHIVED}), ordered by {@code deployedAt} descending. Used by
     * rollback to restore serving to the version that was live immediately before the current one.
     */
    Optional<ModelVersion> findFirstByStatusOrderByDeployedAtDesc(ModelStatus status);
}
