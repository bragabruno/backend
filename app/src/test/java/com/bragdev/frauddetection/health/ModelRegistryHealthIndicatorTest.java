package com.bragdev.frauddetection.health;

import com.bragdev.frauddetection.common.enums.ModelStatus;
import com.bragdev.frauddetection.common.model.ModelVersion;
import com.bragdev.frauddetection.common.repository.ModelVersionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModelRegistryHealthIndicatorTest {

    @Mock
    private ModelVersionRepository modelVersionRepository;

    @Test
    void upWhenAModelIsDeployed() {
        ModelVersion deployed = ModelVersion.builder()
                .version("xgb-2026-02")
                .status(ModelStatus.DEPLOYED)
                .deployedAt(Instant.parse("2026-02-01T00:00:00Z"))
                .build();
        when(modelVersionRepository.findByStatus(ModelStatus.DEPLOYED)).thenReturn(Optional.of(deployed));

        Health health = new ModelRegistryHealthIndicator(modelVersionRepository).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("deployedVersion", "xgb-2026-02");
    }

    @Test
    void downWhenNoModelIsDeployed() {
        when(modelVersionRepository.findByStatus(ModelStatus.DEPLOYED)).thenReturn(Optional.empty());

        Health health = new ModelRegistryHealthIndicator(modelVersionRepository).health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("reason");
    }
}
