package com.bragdev.frauddetection.casemanagement.service;

import com.bragdev.frauddetection.common.enums.CaseStatus;

import java.util.Map;
import java.util.Set;

public final class CaseStateMachine {

    private static final Map<CaseStatus, Set<CaseStatus>> TRANSITIONS = Map.of(
            CaseStatus.OPEN, Set.of(CaseStatus.ASSIGNED, CaseStatus.ESCALATED, CaseStatus.CLOSED),
            CaseStatus.ASSIGNED, Set.of(CaseStatus.IN_REVIEW, CaseStatus.ESCALATED, CaseStatus.CLOSED),
            CaseStatus.IN_REVIEW, Set.of(CaseStatus.RESOLVED_FRAUD, CaseStatus.RESOLVED_LEGIT, CaseStatus.ESCALATED),
            CaseStatus.ESCALATED, Set.of(CaseStatus.IN_REVIEW, CaseStatus.RESOLVED_FRAUD, CaseStatus.RESOLVED_LEGIT),
            CaseStatus.RESOLVED_FRAUD, Set.of(CaseStatus.CLOSED),
            CaseStatus.RESOLVED_LEGIT, Set.of(CaseStatus.CLOSED),
            CaseStatus.CLOSED, Set.of()
    );

    private CaseStateMachine() {}

    public static boolean canTransition(CaseStatus from, CaseStatus to) {
        Set<CaseStatus> allowed = TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    public static void validate(CaseStatus from, CaseStatus to) {
        if (!canTransition(from, to)) {
            throw new IllegalStateException(
                    "Illegal case transition: " + from + " -> " + to);
        }
    }
}
