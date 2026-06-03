package com.bragdev.frauddetection.common.audit;

import com.bragdev.frauddetection.common.model.AuditEvent;
import com.bragdev.frauddetection.common.repository.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * Hash-chaining service for audit trail tamper-evidence.
 * Each AuditEvent's hash includes the previous event's hash, creating a chain.
 * Periodic verification detects any tampering of historical records.
 */
@Service
public class AuditChainService {

    private static final Logger log = LoggerFactory.getLogger(AuditChainService.class);

    private final AuditEventRepository auditEventRepository;

    public AuditChainService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    /**
     * Computes a SHA-256 hash of the audit event fields including the previous hash.
     */
    public String computeHash(AuditEvent event, String previousHash) {
        try {
            String data = String.join("|",
                    event.getId().toString(),
                    event.getActor(),
                    event.getAction(),
                    event.getTargetType(),
                    event.getTargetId() != null ? event.getTargetId().toString() : "",
                    event.getAfter() != null ? event.getAfter() : "",
                    previousHash != null ? previousHash : ""
            );
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Scheduled verification job that checks chain integrity.
     * Logs a warning if any break in the chain is detected.
     */
    @Scheduled(fixedDelayString = "${fraud.audit.verify-interval-ms:3600000}")
    public void verifyChain() {
        List<AuditEvent> allEvents = auditEventRepository.findAll();
        if (allEvents.size() < 2) {
            return;
        }

        String previousHash = null;
        int breaks = 0;
        for (int i = 0; i < allEvents.size(); i++) {
            AuditEvent event = allEvents.get(i);
            if (event.getCorrelationId() != null && event.getCorrelationId().startsWith("hash:")) {
                String storedHash = event.getCorrelationId().substring(5);
                String computedHash = computeHash(event, previousHash);
                if (!storedHash.equals(computedHash)) {
                    log.warn("Audit chain break detected at event {}: stored={}, computed={}",
                            event.getId(), storedHash, computedHash);
                    breaks++;
                }
                previousHash = storedHash;
            } else {
                previousHash = null;
            }
        }

        if (breaks > 0) {
            log.error("AUDIT INTEGRITY: {} chain breaks detected in {} events", breaks, allEvents.size());
        } else {
            log.debug("Audit chain verified: {} events, no breaks", allEvents.size());
        }
    }
}
