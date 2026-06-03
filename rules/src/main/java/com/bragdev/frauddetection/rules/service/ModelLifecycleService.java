package com.bragdev.frauddetection.rules.service;

import com.bragdev.frauddetection.common.config.ResourceNotFoundException;
import com.bragdev.frauddetection.common.enums.ModelStatus;
import com.bragdev.frauddetection.common.event.ModelDeployedEvent;
import com.bragdev.frauddetection.common.model.ModelVersion;
import com.bragdev.frauddetection.common.outbox.OutboxService;
import com.bragdev.frauddetection.common.repository.ModelVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Model lifecycle transitions that must change persistent state and emit a domain event atomically.
 *
 * <p>Rollback (FRAUD-081) implements the {@code DEPLOYED ──regression──▶ ROLLED_BACK} /
 * {@code ARCHIVED ──restore──▶ DEPLOYED} edges of the model state machine: it retires the regressed
 * version and re-points serving to the last-known-good version, then publishes a
 * {@code fraud.model.deployed} event through the transactional outbox so the ml-service hot-swaps
 * its in-memory serving model (FRAUD-087). State change and event are committed in one transaction;
 * the {@code OutboxRelay} guarantees the event reaches Kafka.
 */
@Service
public class ModelLifecycleService {

    static final String TOPIC_MODEL_DEPLOYED = "fraud.model.deployed";
    static final String EVENT_TYPE = "ModelDeployed";

    private static final Logger log = LoggerFactory.getLogger(ModelLifecycleService.class);

    private final ModelVersionRepository modelVersionRepository;
    private final OutboxService outboxService;

    public ModelLifecycleService(ModelVersionRepository modelVersionRepository, OutboxService outboxService) {
        this.modelVersionRepository = modelVersionRepository;
        this.outboxService = outboxService;
    }

    /**
     * Rolls back the currently-deployed model, restoring the last-known-good version to serving.
     *
     * @param deployedModelId id of the model to roll back; must be in {@link ModelStatus#DEPLOYED}
     * @return the restored (now {@link ModelStatus#DEPLOYED}) last-known-good version
     * @throws ResourceNotFoundException if no model exists with that id (404)
     * @throws IllegalStateException     if the model is not currently deployed, or there is no prior
     *                                   version to restore (409)
     */
    @Transactional
    public ModelVersion rollback(UUID deployedModelId) {
        ModelVersion regressed = modelVersionRepository.findById(deployedModelId)
                .orElseThrow(() -> new ResourceNotFoundException("Model", deployedModelId.toString()));

        if (regressed.getStatus() != ModelStatus.DEPLOYED) {
            throw new IllegalStateException(
                    "Model must be DEPLOYED to rollback, current: " + regressed.getStatus());
        }

        ModelVersion lastKnownGood = modelVersionRepository
                .findFirstByStatusOrderByDeployedAtDesc(ModelStatus.ARCHIVED)
                .orElseThrow(() -> new IllegalStateException(
                        "No prior model version to roll back to from " + regressed.getVersion()));

        regressed.setStatus(ModelStatus.ROLLED_BACK);
        modelVersionRepository.save(regressed);

        lastKnownGood.setStatus(ModelStatus.DEPLOYED);
        lastKnownGood.setDeployedAt(Instant.now());
        modelVersionRepository.save(lastKnownGood);

        ModelDeployedEvent event = new ModelDeployedEvent(
                lastKnownGood.getId(),
                lastKnownGood.getVersion(),
                ModelDeployedEvent.REASON_ROLLBACK,
                regressed.getId(),
                lastKnownGood.getDeployedAt());
        outboxService.publish(TOPIC_MODEL_DEPLOYED, lastKnownGood.getId().toString(), EVENT_TYPE, event);

        log.info("Rolled back model {} ({}); restored last-known-good {} ({})",
                regressed.getVersion(), regressed.getId(),
                lastKnownGood.getVersion(), lastKnownGood.getId());

        return lastKnownGood;
    }
}
