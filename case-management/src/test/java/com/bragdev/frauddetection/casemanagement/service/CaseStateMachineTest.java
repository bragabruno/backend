package com.bragdev.frauddetection.casemanagement.service;

import com.bragdev.frauddetection.common.enums.CaseStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CaseStateMachineTest {

    @Test
    void openCanTransitionToAssigned() {
        assertThat(CaseStateMachine.canTransition(CaseStatus.OPEN, CaseStatus.ASSIGNED)).isTrue();
    }

    @Test
    void openCanTransitionToEscalated() {
        assertThat(CaseStateMachine.canTransition(CaseStatus.OPEN, CaseStatus.ESCALATED)).isTrue();
    }

    @Test
    void openCanTransitionToClosed() {
        assertThat(CaseStateMachine.canTransition(CaseStatus.OPEN, CaseStatus.CLOSED)).isTrue();
    }

    @Test
    void openCannotTransitionToResolvedFraud() {
        assertThat(CaseStateMachine.canTransition(CaseStatus.OPEN, CaseStatus.RESOLVED_FRAUD)).isFalse();
    }

    @Test
    void assignedCanTransitionToInReview() {
        assertThat(CaseStateMachine.canTransition(CaseStatus.ASSIGNED, CaseStatus.IN_REVIEW)).isTrue();
    }

    @Test
    void inReviewCanTransitionToResolvedFraud() {
        assertThat(CaseStateMachine.canTransition(CaseStatus.IN_REVIEW, CaseStatus.RESOLVED_FRAUD)).isTrue();
    }

    @Test
    void inReviewCanTransitionToResolvedLegit() {
        assertThat(CaseStateMachine.canTransition(CaseStatus.IN_REVIEW, CaseStatus.RESOLVED_LEGIT)).isTrue();
    }

    @Test
    void resolvedFraudCanTransitionToClosed() {
        assertThat(CaseStateMachine.canTransition(CaseStatus.RESOLVED_FRAUD, CaseStatus.CLOSED)).isTrue();
    }

    @Test
    void closedCannotTransitionToAnything() {
        for (CaseStatus target : CaseStatus.values()) {
            assertThat(CaseStateMachine.canTransition(CaseStatus.CLOSED, target)).isFalse();
        }
    }

    @Test
    void validateThrowsOnIllegalTransition() {
        assertThatThrownBy(() -> CaseStateMachine.validate(CaseStatus.OPEN, CaseStatus.RESOLVED_FRAUD))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Illegal case transition");
    }

    @Test
    void validateDoesNotThrowOnLegalTransition() {
        CaseStateMachine.validate(CaseStatus.OPEN, CaseStatus.ASSIGNED);
    }
}
