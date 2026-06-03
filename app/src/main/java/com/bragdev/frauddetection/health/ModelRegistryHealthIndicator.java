package com.bragdev.frauddetection.health;

import com.bragdev.frauddetection.common.enums.ModelStatus;
import com.bragdev.frauddetection.common.repository.ModelVersionRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Reports whether a fraud model is loaded — i.e. a {@link ModelStatus#DEPLOYED} version exists in the
 * registry. Wired into the {@code readiness} health group (FRAUD-124) so the instance is only routed
 * traffic once a model is live, and surfaces the deployed version as a detail for operators.
 *
 * <p>The bean name yields the {@code modelRegistry} health key. Profiles that run without a datasource
 * tolerate its absence via {@code management.endpoint.health.group.*} membership validation being off.
 */
@Component("modelRegistryHealthIndicator")
public class ModelRegistryHealthIndicator implements HealthIndicator {

    private final ModelVersionRepository modelVersionRepository;

    public ModelRegistryHealthIndicator(ModelVersionRepository modelVersionRepository) {
        this.modelVersionRepository = modelVersionRepository;
    }

    @Override
    public Health health() {
        return modelVersionRepository.findByStatus(ModelStatus.DEPLOYED)
                .map(model -> Health.up()
                        .withDetail("deployedVersion", model.getVersion())
                        .withDetail("deployedAt", String.valueOf(model.getDeployedAt()))
                        .build())
                .orElseGet(() -> Health.down()
                        .withDetail("reason", "no model version is DEPLOYED")
                        .build());
    }
}
