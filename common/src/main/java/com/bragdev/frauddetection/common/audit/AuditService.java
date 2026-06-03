package com.bragdev.frauddetection.common.audit;

import com.bragdev.frauddetection.common.model.AuditEvent;
import com.bragdev.frauddetection.common.repository.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Centralized audit logging service for all security- and case-relevant actions.
 * Provides a consistent cross-service helper for writing AuditEvents.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public AuditEvent log(String actor, String action, String targetType, UUID targetId) {
        return log(actor, action, targetType, targetId, null, null, null);
    }

    public AuditEvent log(String actor, String action, String targetType, UUID targetId, String after) {
        return log(actor, action, targetType, targetId, null, after, null);
    }

    public AuditEvent log(String actor, String action, String targetType, UUID targetId,
                          String before, String after, String correlationId) {
        AuditEvent event = AuditEvent.builder()
                .actor(actor)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .before(before)
                .after(after)
                .correlationId(correlationId)
                .build();
        AuditEvent saved = auditEventRepository.save(event);
        log.debug("Audit: {} {} {} {}", actor, action, targetType, targetId);
        return saved;
    }

    public List<AuditEvent> getAuditTrail(String targetType, UUID targetId) {
        return auditEventRepository.findByTargetTypeAndTargetId(targetType, targetId);
    }

    public List<AuditEvent> getAuditTrailByCorrelationId(String correlationId) {
        return auditEventRepository.findByCorrelationId(correlationId);
    }
}
