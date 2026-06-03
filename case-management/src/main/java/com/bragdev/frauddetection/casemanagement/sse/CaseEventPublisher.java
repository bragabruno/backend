package com.bragdev.frauddetection.casemanagement.sse;

import com.bragdev.frauddetection.common.model.FraudCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class CaseEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CaseEventPublisher.class);
    private static final long SSE_TIMEOUT_MS = 3_600_000L;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    public void publishCaseCreated(FraudCase fraudCase) {
        sendEvent("case_created", fraudCase);
    }

    public void publishCaseAssigned(FraudCase fraudCase) {
        sendEvent("case_assigned", fraudCase);
    }

    public void publishCaseUpdated(FraudCase fraudCase) {
        sendEvent("case_updated", fraudCase);
    }

    public void publishCaseResolved(FraudCase fraudCase) {
        sendEvent("case_resolved", fraudCase);
    }

    @Scheduled(fixedDelay = 30_000)
    public void sendHeartbeat() {
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data(""));
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        if (!dead.isEmpty()) {
            emitters.removeAll(dead);
            log.debug("Removed {} disconnected SSE emitters during heartbeat", dead.size());
        }
    }

    private void sendEvent(String eventName, FraudCase fraudCase) {
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(new CaseEvent(
                                eventName,
                                fraudCase.getId().toString(),
                                fraudCase.getTransactionId().toString(),
                                fraudCase.getStatus().name(),
                                fraudCase.getSeverity().name(),
                                fraudCase.getAssigneeId() != null ? fraudCase.getAssigneeId().toString() : null)));
            } catch (IOException e) {
                log.debug("Dropping disconnected SSE emitter for event {}: {}", eventName, e.getMessage());
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
    }

    public record CaseEvent(
            String eventType,
            String caseId,
            String transactionId,
            String status,
            String severity,
            String assigneeId
    ) {}
}
