package com.bragdev.frauddetection.common.repository;

import com.bragdev.frauddetection.common.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    List<AuditEvent> findByTargetTypeAndTargetId(String targetType, UUID targetId);

    List<AuditEvent> findByCorrelationId(String correlationId);

    List<AuditEvent> findByActor(String actor);
}
