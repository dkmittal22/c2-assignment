package com.c2assignment.common;

import com.c2assignment.ticket.TicketStatus;

/**
 * Raised when a transition is not allowed by the state machine.
 * Maps to HTTP 409 INVALID_TRANSITION (spec/state-machine.md §8).
 */
public class InvalidTransitionException extends RuntimeException {

    private final TicketStatus from;
    private final TicketStatus to;

    public InvalidTransitionException(TicketStatus from, TicketStatus to) {
        super("Status transition from " + from + " to " + to + " is not allowed");
        this.from = from;
        this.to = to;
    }

    public TicketStatus getFrom() {
        return from;
    }

    public TicketStatus getTo() {
        return to;
    }
}