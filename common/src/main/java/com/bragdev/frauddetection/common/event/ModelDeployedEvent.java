package com.bragdev.frauddetection.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published to the {@code fraud.model.deployed} topic whenever a model version becomes the live
 * serving model. The ml-service consumes this to atomically hot-swap its in-memory serving model
 * (FRAUD-087); the fraud engine consumes it to tag scores with the active model version.
 *
 * <p>{@code reason} distinguishes a forward deploy ({@code "DEPLOY"}) from a rollback that restores
 * the last-known-good version ({@code "ROLLBACK"}). On a deploy {@code previousVersionId} is the
 * superseded version; on a rollback it is the regressed version being rolled back from.
 */
public record ModelDeployedEvent(
        UUID modelVersionId,
        String version,
        String reason,
        UUID previousVersionId,
        Instant deployedAt
) {
    public static final String REASON_DEPLOY = "DEPLOY";
    public static final String REASON_ROLLBACK = "ROLLBACK";
}
