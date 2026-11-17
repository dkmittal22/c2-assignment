package com.c2assignment.ticket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the full transition matrix (spec/state-machine.md §4-6,
 * test-strategy §4.1). This is the domain rule the transition API enforces.
 */
class TicketStatusTransitionTest {

    @ParameterizedTest(name = "{0} -> {1} is allowed")
    @CsvSource({
            "OPEN, IN_PROGRESS",
            "OPEN, CANCELLED",
            "IN_PROGRESS, RESOLVED",
            "IN_PROGRESS, CANCELLED",
            "RESOLVED, CLOSED"
    })
    void validTransitionsAreAllowed(String from, String to) {
        assertThat(TicketStatusTransitionPolicy.isAllowed(
                TicketStatus.valueOf(from), TicketStatus.valueOf(to))).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1} is rejected")
    @CsvSource({
            "OPEN, RESOLVED",
            "OPEN, CLOSED",
            "OPEN, OPEN",
            "IN_PROGRESS, CLOSED",
            "IN_PROGRESS, OPEN",
            "IN_PROGRESS, IN_PROGRESS",
            "RESOLVED, OPEN",
            "RESOLVED, IN_PROGRESS",
            "RESOLVED, CANCELLED",
            "RESOLVED, RESOLVED",
            "CLOSED, OPEN",
            "CLOSED, IN_PROGRESS",
            "CLOSED, RESOLVED",
            "CLOSED, CANCELLED",
            "CLOSED, CLOSED",
            "CANCELLED, OPEN",
            "CANCELLED, IN_PROGRESS",
            "CANCELLED, RESOLVED",
            "CANCELLED, CLOSED",
            "CANCELLED, CANCELLED"
    })
    void invalidTransitionsAreRejected(String from, String to) {
        assertThat(TicketStatusTransitionPolicy.isAllowed(
                TicketStatus.valueOf(from), TicketStatus.valueOf(to))).isFalse();
    }

    @Test
    @DisplayName("Terminal states (CLOSED, CANCELLED) have no outbound transitions")
    void terminalStatesHaveNoOutboundTransitions() {
        for (TicketStatus target : TicketStatus.values()) {
            assertThat(TicketStatusTransitionPolicy.isAllowed(TicketStatus.CLOSED, target)).isFalse();
            assertThat(TicketStatusTransitionPolicy.isAllowed(TicketStatus.CANCELLED, target)).isFalse();
        }
    }

    @Test
    @DisplayName("Every self-transition is rejected")
    void selfTransitionsAreRejected() {
        for (TicketStatus status : TicketStatus.values()) {
            assertThat(TicketStatusTransitionPolicy.isAllowed(status, status)).isFalse();
        }
    }

    @Test
    @DisplayName("Null arguments are never allowed")
    void nullArgumentsAreRejected() {
        assertThat(TicketStatusTransitionPolicy.isAllowed(null, TicketStatus.OPEN)).isFalse();
        assertThat(TicketStatusTransitionPolicy.isAllowed(TicketStatus.OPEN, null)).isFalse();
        assertThat(TicketStatusTransitionPolicy.isAllowed(null, null)).isFalse();
    }
}