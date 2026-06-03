package com.bragdev.frauddetection.rules.service;

import com.bragdev.frauddetection.common.config.ResourceNotFoundException;
import com.bragdev.frauddetection.common.enums.ModelStatus;
import com.bragdev.frauddetection.common.event.ModelDeployedEvent;
import com.bragdev.frauddetection.common.model.ModelVersion;
import com.bragdev.frauddetection.common.outbox.OutboxService;
import com.bragdev.frauddetection.common.repository.ModelVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModelLifecycleServiceTest {

    @Mock private ModelVersionRepository modelVersionRepository;
    @Mock private OutboxService outboxService;

    private ModelLifecycleService service;

    private static final UUID DEPLOYED_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID PRIOR_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @BeforeEach
    void setUp() {
        service = new ModelLifecycleService(modelVersionRepository, outboxService);
    }

    private static ModelVersion model(UUID id, String version, ModelStatus status, Instant deployedAt) {
        return ModelVersion.builder()
                .id(id)
                .version(version)
                .status(status)
                .deployedAt(deployedAt)
                .build();
    }

    @Test
    void rollbackRetiresCurrentAndRestoresLastKnownGood() {
        ModelVersion regressed = model(DEPLOYED_ID, "xgb-2026-02", ModelStatus.DEPLOYED, Instant.now());
        ModelVersion prior = model(PRIOR_ID, "xgb-2026-01", ModelStatus.ARCHIVED,
                Instant.parse("2026-01-15T00:00:00Z"));

        when(modelVersionRepository.findById(DEPLOYED_ID)).thenReturn(Optional.of(regressed));
        when(modelVersionRepository.findFirstByStatusOrderByDeployedAtDesc(ModelStatus.ARCHIVED))
                .thenReturn(Optional.of(prior));

        ModelVersion restored = service.rollback(DEPLOYED_ID);

        // Regressed version retired, last-known-good restored to serving.
        assertThat(regressed.getStatus()).isEqualTo(ModelStatus.ROLLED_BACK);
        assertThat(restored).isSameAs(prior);
        assertThat(restored.getStatus()).isEqualTo(ModelStatus.DEPLOYED);
        assertThat(restored.getDeployedAt()).isNotNull();
        verify(modelVersionRepository).save(regressed);
        verify(modelVersionRepository).save(prior);
    }

    @Test
    void rollbackPublishesModelDeployedEventForRestoredVersion() {
        ModelVersion regressed = model(DEPLOYED_ID, "xgb-2026-02", ModelStatus.DEPLOYED, Instant.now());
        ModelVersion prior = model(PRIOR_ID, "xgb-2026-01", ModelStatus.ARCHIVED,
                Instant.parse("2026-01-15T00:00:00Z"));

        when(modelVersionRepository.findById(DEPLOYED_ID)).thenReturn(Optional.of(regressed));
        when(modelVersionRepository.findFirstByStatusOrderByDeployedAtDesc(ModelStatus.ARCHIVED))
                .thenReturn(Optional.of(prior));

        service.rollback(DEPLOYED_ID);

        ArgumentCaptor<ModelDeployedEvent> payload = ArgumentCaptor.forClass(ModelDeployedEvent.class);
        verify(outboxService).publish(
                eq(ModelLifecycleService.TOPIC_MODEL_DEPLOYED),
                eq(PRIOR_ID.toString()),
                eq(ModelLifecycleService.EVENT_TYPE),
                payload.capture());

        ModelDeployedEvent event = payload.getValue();
        assertThat(event.modelVersionId()).isEqualTo(PRIOR_ID);
        assertThat(event.version()).isEqualTo("xgb-2026-01");
        assertThat(event.reason()).isEqualTo(ModelDeployedEvent.REASON_ROLLBACK);
        assertThat(event.previousVersionId()).isEqualTo(DEPLOYED_ID);
        assertThat(event.deployedAt()).isEqualTo(prior.getDeployedAt());
    }

    @Test
    void rollbackUnknownModelThrowsNotFound() {
        when(modelVersionRepository.findById(DEPLOYED_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rollback(DEPLOYED_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(outboxService, never()).publish(any(), any(), any(), any());
    }

    @Test
    void rollbackNonDeployedModelThrowsConflict() {
        ModelVersion approved = model(DEPLOYED_ID, "xgb-2026-02", ModelStatus.APPROVED, null);
        when(modelVersionRepository.findById(DEPLOYED_ID)).thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> service.rollback(DEPLOYED_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be DEPLOYED");

        verify(modelVersionRepository, never()).save(any());
        verify(outboxService, never()).publish(any(), any(), any(), any());
    }

    @Test
    void rollbackWithNoPriorVersionThrowsConflictAndDoesNotMutate() {
        ModelVersion regressed = model(DEPLOYED_ID, "xgb-2026-01", ModelStatus.DEPLOYED, Instant.now());
        when(modelVersionRepository.findById(DEPLOYED_ID)).thenReturn(Optional.of(regressed));
        when(modelVersionRepository.findFirstByStatusOrderByDeployedAtDesc(ModelStatus.ARCHIVED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rollback(DEPLOYED_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No prior model version");

        // The regressed model must remain DEPLOYED — no half-applied rollback.
        assertThat(regressed.getStatus()).isEqualTo(ModelStatus.DEPLOYED);
        verify(modelVersionRepository, never()).save(any());
        verify(outboxService, never()).publish(any(), any(), any(), any());
    }
}
