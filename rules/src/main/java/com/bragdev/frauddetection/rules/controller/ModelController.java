package com.bragdev.frauddetection.rules.controller;

import com.bragdev.frauddetection.common.enums.ModelStatus;
import com.bragdev.frauddetection.common.model.ModelVersion;
import com.bragdev.frauddetection.common.repository.ModelVersionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/models")
@Tag(name = "Model Lifecycle", description = "Model version management and deployment approval")
public class ModelController {

    private final ModelVersionRepository modelVersionRepository;

    public ModelController(ModelVersionRepository modelVersionRepository) {
        this.modelVersionRepository = modelVersionRepository;
    }

    @GetMapping
    @Operation(summary = "List all model versions")
    public List<ModelVersion> listModels() {
        return modelVersionRepository.findAll();
    }

    @GetMapping("/deployed")
    @Operation(summary = "Get current deployed model version")
    public ModelVersion getDeployedModel() {
        return modelVersionRepository.findByStatus(ModelStatus.DEPLOYED)
                .orElseThrow(() -> new RuntimeException("No deployed model found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new model version")
    public ModelVersion registerModel(@RequestBody RegisterModelRequest request) {
        ModelVersion model = ModelVersion.builder()
                .version(request.version())
                .mlflowRunId(request.mlflowRunId())
                .status(ModelStatus.REGISTERED)
                .metrics(request.metrics())
                .build();
        return modelVersionRepository.save(model);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve model for deployment (Admin only)")
    public ModelVersion approveModel(@PathVariable UUID id) {
        ModelVersion model = modelVersionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Model not found: " + id));

        if (model.getStatus() != ModelStatus.REGISTERED) {
            throw new IllegalStateException(
                    "Model must be REGISTERED to approve, current: " + model.getStatus());
        }

        model.setStatus(ModelStatus.APPROVED);
        return modelVersionRepository.save(model);
    }

    @PostMapping("/{id}/deploy")
    @Operation(summary = "Deploy model to production (retires previous)")
    public ModelVersion deployModel(@PathVariable UUID id) {
        ModelVersion model = modelVersionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Model not found: " + id));

        if (model.getStatus() != ModelStatus.APPROVED) {
            throw new IllegalStateException(
                    "Model must be APPROVED to deploy, current: " + model.getStatus());
        }

        // Retire current deployed model
        modelVersionRepository.findByStatus(ModelStatus.DEPLOYED).ifPresent(current -> {
            current.setStatus(ModelStatus.ARCHIVED);
            modelVersionRepository.save(current);
        });

        model.setStatus(ModelStatus.DEPLOYED);
        model.setDeployedAt(Instant.now());
        return modelVersionRepository.save(model);
    }

    @PostMapping("/{id}/rollback")
    @Operation(summary = "Rollback a deployed model")
    public ModelVersion rollbackModel(@PathVariable UUID id) {
        ModelVersion model = modelVersionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Model not found: " + id));

        if (model.getStatus() != ModelStatus.DEPLOYED) {
            throw new IllegalStateException(
                    "Model must be DEPLOYED to rollback, current: " + model.getStatus());
        }

        model.setStatus(ModelStatus.ROLLED_BACK);
        return modelVersionRepository.save(model);
    }

    public record RegisterModelRequest(
            String version,
            String mlflowRunId,
            Map<String, Double> metrics
    ) {}
}
